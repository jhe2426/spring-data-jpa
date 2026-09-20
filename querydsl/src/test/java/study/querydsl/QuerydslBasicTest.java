package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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
        - Java 객체의 실제 값과, 쿼리에서 사용할 엔티티.속성이라는 표현을 구분해야 했기 때문에 QueryDSL은 Q클래스라는 별도의 쿼리용 메타 모델을 만든 것
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

    /*
        JPQL이 제공하는 모든 검색 조건 제공
        member.username.eq("member1") // username = 'member1'
        member.username.ne("member1") // username != 'member1'
        member.username.eq("member1").not() // username != 'member1'

        member.username.isNotNull() // 이름이 is not null

        member.age.in(10, 20) // age in (10,20)
        member.age.notIn(10, 20) // age not in (10, 20)
        member.age.between(10, 30) // between 10, 30

        member.age.goe(30) // age >= 30
        member.age.gt(30) // age > 30
        member.age.loe(30) // age <= 30
        member.age.lt(30) // age < 30

        member.username.like("member%") // like 검색, % 위치를 원하는 곳에 넣어서 검색하면 됨
        member.username.contains("member") // like %member% 검색
        member.username.startsWith("member") // like member% 검색
    */
    @Test
    public void search() {
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    /*
        where() 메서드에 파라미터를 여러 개 넣으면 and 조건으로 다 자동으로 붙여줌
        위의 테스트처럼 .and()를 직접 작성해서 표현해도 되고 where 메서드의 조건문을 전달해도 and 조건으로 조립을 해줌
        실무에서는 where()에 매개변수를 전달하는 것으로 and 조건을 조립한다고 함 왜냐하면 null을 입력되면 해당 값은 무시해주고 조립을 해주기 때문에
        그래서 동적 쿼리문을 작성하기에 너무 편리하게 구현할 수 있기 때문이다.
    */
    @Test
    public void searchAndParam() {
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
        결과 조회
        - fetch(): 리스트 조회, 데이터 없으면 빈 리스트 반환
        - fetchOne(): 단 건 조회
            - 결과가 없으면 : null
            - 결과가 둘 이상이면: com.querydsl.core.NonUniqueResultException
        - fetchFirst(): limit(1).fetchOne()

        fetchResults(), fetchCount()를 향후에는 지원하지 않음
            단순한 쿼리에서는 해당 기능들이 잘 동작하지만, 복잡한 쿼리에서는 제대로 동작하지 않기 때문에
        - fetchResults(): 페이징 정보 포함, total count 쿼리 추가 실행
        - fetchCount(): count 쿼리로 변경해서 count 수 조회
    */
    
    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /*
        회원 정렬 순서
        1. 회원 나이 내림차순(desc)
        2. 회원 이름 올림차순(asc)
        단, 2번에서 회원 이름이 없으면 마지막에 출력하도록 정렬(nulls last)
    */
    @Test
    public void sort() {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // nullsLast(), nullsFirst() 이렇게 존재함
                .fetch();

        // then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        // then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        // given
        PageRequest pageable = PageRequest.of(1, 2);

        // when
        List<Member> content = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member);

        Page<Member> result = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getPageable().getOffset()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    public void count() {
        // when
        Long totalCount = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        // then
        assertThat(totalCount).isEqualTo(4);
    }

    @Test
    public void aggregation() {
        // when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // then
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
        팀의 이름과 각 팀의 평균 연령을 구해라
    */
    @Test
    public void group() {
        // when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        // then
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
}
