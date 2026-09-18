package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // when
        String qlString = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
        Q클래스는 왜 컴파일을 거쳐야지만 생성이 될까?
        - 컴파일 과정이 이루어져야지만 QueryDSL이 자동 생성하는 Java 소스이기 때문
        - QueryDSL Annotation Processor가 컴파일 시점에 @Entity 클래스를 분석해서 자동으로 만들어 주기 때문

        queryDSL으로 쿼리문을 작성할 때 엔티티 클래스를 전달받지 않고 Q클래스를 전달받아야 할까?
        - QueryDSL이 필요한 것은 실제 데이터가 들어 있는 엔티티 객체가 아니라, 쿼리 안에서 테이블과 컬럼을 표현할 수 있는 객체를 필요로 하기 때문
        - QueryDSL은 엔티티의 필드를 실제 Java 값으로 다루는 대신, 쿼리 안에서 특정 엔티티의 속성을 가리키는 Path 객체로 표현한다.
            그리고 eq(), gt(), like() 같은 메서드를 호출하면 실제 Java 연산을 수행하는 것이 아니라,
            age > 20, username like ...와 같은 쿼리 조건을 나타내는 표현식을 생성한다.
            QueryDSL은 이 표현식들을 모아서 최종적으로 JPQL/SQL을 만들어 실행한다.
        - Java 객체의 실제 값과, 쿼리에서 사용할 엔티티.속성이라는 표현을 구분해야 했기 때문에 QueryDSL은 Q클래스라는 별도의 쿼리용 메타모델을 만든 것
    */
    @Test
    public void startQuerydsl() {
        // when
        /*
            별칭 직접 지정, 해당 별칭은 JPQL이 나갈 때 Member 엔티티의 별칭으로 사용 됨
            QMember m = new QMember("m");
            별칭을 직접 지정해서 QueryDSL을 날리는 경우는 자기 테이블과 조인할 때 같은 테이블의 별칭이 서로 달라야 할 때 사용
        */

//        QMember m = QMember.member; // 기본 인스턴스 사용

        Member findMember = queryFactory
                /*
                    Q클래스 static import 방식
                    select(QMember.member) 이렇게 접근한 뒤 QMember를 static import하면 됨
                */
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
