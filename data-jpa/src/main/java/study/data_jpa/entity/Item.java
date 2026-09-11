package study.data_jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    [스프링 데이터 JPA에서 새로운 엔티티를 구별하는 방법]
    save() 메서드
    - 새로운 엔티티면 저장(persist)
    - 새로운 엔티티가 아니면 병합(merge)

    새로운 엔티티를 판단하는 기본 전략
    - 식별자가 객체일 때 null로 판단
    - 식별자가 자바 기본 타입일 때 0으로 판단
    - Persistable 인터페이스로 구현해서 판단 로직 변경 가능

    참고
    - JPA 식별자 생성 전략이 @GenerateValue이면 save() 호출 시점에 식별자가 없으므로 새로운 엔티티로 인색해서 정상 동작한다.
    - 그런데 JPA 식별자 생성 전략이 @Id만 사용해서 직접 할당이면 이미 식별자 값이 있는 상태로 save()를 호출하게 된다.
    - 따라서 이 경우 merge()가 호출되게 된다. merge()는 우선 DB를 호출해서 값을 확인하고, DB에 없으면 새로운 엔티티로 인지하므로
    - 매우 비효율 적이다. 따라서 Persistable를 사용해서 새로운 엔티티 확인 여부를 직접 구현하는게 효과적이다.
    - 참고로 등록시간(@CreatedDate)을 조합해서 사용하면 이 필드로 새로운 엔티티 여부를 편리하게 확인할 수 있다.
        (@CreatedDate에 값이 없으면 새로운 엔티티로 판단)
*/
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item implements Persistable<String> {

/*
    @Id @GeneratedValue
    private Long id;
*/

    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private String id;

    public Item(String id) {
        this.id = id;
    }

    @Override
    public @Nullable String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return createdDate == null;
    }
}
