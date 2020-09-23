package info.m2sj.reactivepostgresql;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("m2sj_test")
public class RNumber {
    @Id
    private Long id;

    private Float randomNum;
}
