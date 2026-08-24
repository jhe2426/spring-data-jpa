package study.data_jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

    protected Member(Long id) {
        this.id = id;
    }

    public Member(String username) {
        this.username = username;
    }

    public void changeUsername(String uesrname) {
        this.username = username;
    }
}
