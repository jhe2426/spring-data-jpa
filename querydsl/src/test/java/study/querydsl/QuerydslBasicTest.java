package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
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
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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

    /*
        기본 조인
        - 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번재 파라미터에 별칭으로 사용할 Q타입을 지정하면 됨
        - join(조인 대상, 별칭으로 사용할 Q타입)
    */
    // 팀 A에 소속된 모든 회원 조회
    @Test
    public void join() {
        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
        세타 조인 예제
        회원의 이름이 팀 이름과 같은 회원 조회
    */
    @Test
    public void theta_join() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }



    /*
        예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
        JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
    */
    @Test
    public void join_on_filtering1() {
        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
        on절을 활용해서 조인 대상을 필터링할 때, 외부조인이 아니라 내부조인을 사용하면, where 절에서 필터링하는 것과 기능이 동일하다.
        따라서 on절을 활용한 조인 대상 필터링을 사용할 때, 내부조인이면 익숙한 where절로 해결하고, 외부조인이 필요한 경우에만 on 절을 활용해서
        필터링을 하면 된다.
    */
    @Test
    public void join_on_filtering2() {
        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
//                .on(team.name.eq("teamA"))
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
       연관관계가 없는 엔티티 외부 조인
       회원의 이름이 팀 이름과 같은 대상 외부 조인

       하이버네이트 5.1부터는 on을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가 됨. 물론 내부 조인도 가능
       주의 조인을 위해서는 각각의 엔티티를 넣어줘야 함
       일반조인: leftJoin(member.team, team) member.team이렇게 넘겨주면 on절에 team.team_id = member.team_id를 자동을 붙여주기 때문
       on조인: from(member).leftJoin(team).on(xxx)
   */
    @Test
    public void join_on_no_relation() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // 조인에 member.team이 아닌 team만 작성을 하게 되면 on절에 team.team_id = member.team_id이 들어가지 않게 됨
                .join(team).on(member.username.eq(team.name))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }


    /*
        서브 쿼리: com.querydsl.jpa.JPAExpressions을 사용하여 구현할 수 있음

        from 절의 서브쿼리 한계
        JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
        당연히 JPQL을 생성하는 Querydsl도 지원하지 않는다.

        from 절의 서브쿼리 해결방안
        1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
        2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
        3. nativeSQL을 사용한다.
    */
    // 나이가 가장 많은 회원 조회
    @Test
    public void subQuery() {
        // when

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    // 나이가 평균 이상인 회원
    @Test
    public void subQueryGoe() {
        // when

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    // 나이가 평균 이상인 회원
    @Test
    public void subQueryIn() {
        // when
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubquery() {
        // when
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    
    @Test
    public void basicCase() {
        // when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        // when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
        member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue()로 문자로 변환할 수 있다.
        이 방법은 ENUM을 처리할 때에도 자주 사용한다.
        엔티티 타입이 enum 타입이면 QueryDSL에서도 해당 타입으로 인식하기 때문에 문자열 연산이 필요한 경우 stringValue()를 사용해 문자열 표현식으로 변환한다.
    */
    @Test
    public void concat() {
        // when
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /*
        프로젝션: select 대상을 의미
        프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
        프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
    */
    @Test
    public void simpleProjection() {
        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void tupleProjection() {
        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }
    
    @Test
    public void findDtoByJPQL() {
        // when
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    /*
        기본 생성자를 통해서 인스턴스를 만든 뒤 setter로 값을 바인딩해줌
    */
    @Test
    public void findDtoBySetter() {
        // when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        // when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        // when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        // when
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDto() {
        // when
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
        QMemberDto를 사용하면 DTO 생성자를 기반으로 Q타입이 생성되므로 컴파일 시점에 타입과 생성자 파라미터를 검증할 수 있어 안전하다.
        하지만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q파일이 생성된다.
        따라서 추후 QueryDSL을 제거하거나 다른 조회 기술로 변경할 경우 조회 코드뿐만 아니라 @QueryProjection이 적용된 DTO까지 수정해야 하는 단점이 있다.
    */
    @Test
    public void findDtoQueryProjection() {
        // when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        // when
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        /*
            BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
            usernameCond값이 항상 null이 아니라면 초기 값또한 넣을 수 있음
            usernameCond값이 null이면 java.lang.IllegalArgumentException: 에러가 발생
        */
        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    public void dynamicQuery_WhereParam() {
        // when
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) :  null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
}
