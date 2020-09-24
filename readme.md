요즘 가장 핫한 패러다임인 Reactive 한 애플리케이션을 만들기 위해서 Spring Web Flux를 이용해 웹 애플리케이션을 만들어도 DB가 Reactive를 지원하지 않으면 DB에서 Blocking되기 때문에 의미가 없다. Redis 같은 cache 나 Mongodb , 카산드라같은 NoSQL 은 조금 일찍 지원하고 있었으나 RDBMS 는 최근에서 1.x 버전이 공식 릴리즈 됐다. 오늘은 R2DBC([Reactive Relational Database Connectivity](https://github.com/r2dbc/))를 이용해 Postgresql 을 저장소로 하는 간단한 테스트를 해보겠다. 

### Spring Boot 2.3.0.M2

이 글을 작성된 날(20.04.02) 기준  Spring Boot 최신 버전은 2.2.6 이다.  Spring Boot 에서 r2dbc를 추상해 놓은 Spring Data R2dbc가 존재한다. 하지만 최신 버전에서는 아직 사용할 수 없고 Spring Boot 2.3.0.M2 버전에서 사용 가능하다. 마일 스톤 버전을 받기 위해서는 플러그와  라이브리러리 Repostiory를 추가해줘야 된다.

```
 <parent>
 	<groupId>org.springframework.boot</groupId>
 	<artifactId>spring-boot-starter-parent</artifactId>
 	<version>2.3.0.M3</version>
 	<relativePath/> <!-- lookup parent from repository -->
 </parent>

-- 중략--
<repositories>
	<repository>
		<id>spring-milestones</id>
		<name>Spring Milestones</name>
		<url>https://repo.spring.io/milestone</url>
	</repository>
</repositories>
<pluginRepositories>
	<pluginRepository>
		<id>spring-milestones</id>
		<name>Spring Milestones</name>
		<url>https://repo.spring.io/milestone</url>
	</pluginRepository>
</pluginRepositories>
```

테스트에 필수적인 라이브러리들을 추가한다. 개발 편의를 위해 lombok도 추가해줬다. 

```
	<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <dependency>
            <groupId>io.r2dbc</groupId>
            <artifactId>r2dbc-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
```

### 테스트 Data 생성

아래의 SQL 문을 참고해서 테스트를 위한 테이블 생성 및 데이터를 생성한다. 1000만 건 정도의 대용량 데이터를 입력한다.

```
#테스트 테이블 생성
create table m2sj_test
(
    random_num double precision,
    id serial not null
        constraint m2sj_test_pk
            primary key
);
#백만건의 테스트 데이터 저장
insert into m2sj_test(random_num)
SELECT random() from generate_series(1, 10000000);
;

```

### R2DBCConfiguration

postgresql Connection Factory를 생성한다. @Repository 등록을 @EnableR2dbcRepositories 선언이 필요하다.

```
@Configuration
@EnableR2dbcRepositories
public class R2DBCConfiguration {
    @Bean
    public PostgresqlConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("127.0.0.1")
                        .database("testdb")
                        .username("testuser")
                        .password("1234").build());
    }
}

```

### RNumber

엔터티로 사용할 RNumber 클래스이다. 테이블명과 클래스명이 다를 경우 @Table 명령어를 이용해서 명시적 선언이 필요하다.

```
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("m2sj_test")
public class RNumber {
    @Id
    private Long id;

    private Float randomNum;
}
```

### RNumberRepository

ReactiveCrudRepository 를 상속받는 RnumberRepository 인터페이스이다. ReactiveCrudRepository 클래스에서 기본적인 조회 메서드를 이미 구현해놨기 때문에 별도의 조회 메서드 선언 없이 사용 가능하다.

```
@Repository
public interface RNumberRepository extends ReactiveCrudRepository<RNumber, Long> {

}
```

### RNumberController

클라이언트 요청을 처리하기 위한 Controller 클래스이다. @GetMapping(value = "", produces = TEXT\_EVENT\_STREAM\_VALUE)  
라인이 중요한데 Media Type을 SSE(Server Send Event) 선언하는 부분이다. 이렇게 선언해야 1천만건의 데이터를 웹에서 즉각적으로 받을 수 있다. Server Send Evnet 에 대한 자세한 내용은 아래 링크를 참고하면 된다.

[http://developer.mozilla.org/en-US/docs/Web/API/Server-sent\_events/Using\_server-sent\_events](http://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)

```
@RestController
@RequestMapping(value = "/api/numbers")
public class RNumberController {
    private final RNumberRepository numberRepository;

    public RNumberController(RNumberRepository numberRepository) {
        this.numberRepository = numberRepository;
    }

    @GetMapping(value = "", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<RNumber> getHome() {
        return numberRepository.findAll();
    }
}
```

### 테스트

브라우저를 열고 http://localhost:8080/api/numbers 을 요청하면 pending 타임 없이 아래와 같은 응답 결과를 바로 확인할 수 있다.

[##_Image|kage@cnS78o/btqJh31BNos/CWkSvVZieNdLXzSzglt9GK/img.png|alignCenter|data-origin-width="0" data-origin-height="0" data-ke-mobilestyle="widthContent"|||_##]

### DatabaseClient

만약 서버에서 서버로 Reactive 한 요청을 한다면 R2dbc 에서는 추상화된 클라이언트를 제공한다. 아래는 간단한 사용법이다. TimeUnit.SECONDS.sleep(10) 라인을 추가한건 일반적인 메인 함수로 실행했기 때문에 결과를 받아오기 전에 메인 쓰레드가 terminated 되는걸 기다리기 위해서이다.

```
    public static void main(String[] args) throws InterruptedException {
        final PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .database("test_db")
                        .username("test_user")
                        .password("1234").build());

        DatabaseClient client = DatabaseClient.create(postgresqlConnectionFactory);

        client.select()
                .from(RNumber.class)
                .fetch().all()
                .subscribe(System.out::println);

        TimeUnit.SECONDS.sleep(10);
    }
```

### 끝으로

r2db 를 이용한 간단한 애플리케이션을 만들어 봤다. postgresql DB에 1천만건의 데이터를 조회 하는데 1초에 대기 시간없이 즉각적인 응답을 확인할 수 있었다. 이런 부분이 리엑티브 프로그래밍의 강력한 부분이라고 생각한다. (개인적으로 마법과 같이 느껴진다.) 아마 리엑티브 프로그래밍 대용량 데이터를 다루는 애플리케이션을 만들 때 여러 부분에서 유용하게 사용될 수 있을 것 같다. 단 리엑티브 패러다임 자체를 완변히 소화하기 위해서는 많은 학습이 필요할 것 같다.  
  
전체 코드는 아래 GIThub 링크에 참고하면 된다.

[github.com/warpgate3/springboot2-r2dbc-postgresql](https://github.com/warpgate3/springboot2-r2dbc-postgresql)

---

### 참고

-   [https://spring.io/projects/spring-data-r2dbc](https://spring.io/projects/spring-data-r2dbc)  
      
    
-   [https://www.baeldung.com/spring-data-r2dbc](https://www.baeldung.com/spring-data-r2dbc)
