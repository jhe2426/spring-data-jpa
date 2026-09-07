package study.data_jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import study.data_jpa.entity.Member;

import java.util.List;

/*
    사용자 정의 구현 클래스 규칙
    - 리포지토리 인터페이스 이름 + Impl
    - 스프링 데이터 JPA가 인식해서 스프링 빈으로 등록
    - 실무에서는 주로 QueryDSL이나 SpringJdbcTemplate을 함께 사용할 때 사용자 정의 리포지토리 기능 자주 사용
    - 항상 사용자 정의 리포지토리가 필요한 것은 아니다. 그냥 임의의 리포지토리를 만들어도 된다. 예를들어
        MemberQueryRepository를 인터페이스가 아닌 클래스로 만들고 스프링 빈으로 등록해서 그냥 직접 사용해도 됨
        물론 이 경우 스프링 데이터 JPA와는 아무런 관계 없이 별도로 동작한다.
    - 스프링 데이터 2.x 부터는 사용자 정의 인터페이스 명 + Impl 방식도 지원
         MemberRepositoryImpl 대신에 MemberRepositoryCustomImpl 같이 구현해도 된다.
         기존 방식보다 이 방식이 사용자 정의 인터페이스 이름과 구현 클래스 이름이 비슷하므로 더 직관적임
          추가로 여러 인터페이스를 분리해서 구현하는 것도 가능하기 때문에 새롭게 변경된 이 방식을 사용하는 것을 더 권장
            과거의 MemberRepositoryImpl 하나에 모든 사용자 정의 기능을 구현하는 방식보다, 기능별 사용자 정의 인터페이스를
            만들고 각각 인터페이스명 + Impl 구현체를 만든 뒤 필요한 Repository에서 조합하는 Fragment 기반 방식을 권장한다.
            여러 사용자 정의 기능을 독립적으로 분리하고 재사용할 수 있기 때문이다.
                public interface MemberSearchRepository {
                    List<Member> search(...);
                }
                public class MemberSearchRepositoryImpl implements MemberSearchRepository {
                    ...
                }

                public interface MemberStatisticsRepository {
                    MemberStatistics getStatistics();
                }

                public class MemberStatisticsRepositoryImpl implements MemberStatisticsRepository {
                    ...
                }

    [사용자 정의 Repository 분리 기준]
    - 사용자 정의 Repository에는 도메인 로직이나 핵심 비즈니스 처리에 필요한 조회 쿼리를 둔다
    - 반면 특정 화면(View/API 응답)에만 필요한 조회는 별도의 조회 전용 Repository 인터페이스로 분리한다.
    - 즉 Repository를 단순히 Member와 관련된 쿼리라는 이유로 모두 한 곳에 모으지 않고, 쿼리의 목적과 변경 이유를 기준으로 분리하여 유지보수성을 높인다.
*/
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}
