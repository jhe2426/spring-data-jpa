package study.data_jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/*
    제네릭 타입
    - T: 엔티티
    - ID: 엔티티의 식별자 타입
    - S: 엔티티와 그 자식 타입

    주요 메서드
    - save(S): 새로운 엔티티는 저장하고 이미 있는 엔티티는 병합한다.
    - delete(T): 엔티티 하나를 삭제한다. 내부에서 EntityManager.remove() 호출
    - findById(ID): 엔티티 하나를 조회한다. 내부에서 EntityManager.find() 호출
    - getOne(ID): 엔티티를 프록시로 조회한다. 내부에서 EntityManager.getReference() 호출
        - 참고: 최신 버전에서 getOne(ID) 대신에 getReferenceById(ID)를 사용하도록 변경됨
        [특징]
            - 일반적으로 메서드를 호출하는 시점에는 SELECT 쿼리가 실행되지 않는다.
            - 엔티티의 실제 필드에 접근하여 프록시가 초기화되는 시점에 SELECT 쿼리가 실행된다.
                - 이때 DB에 해당 ID의 엔티티가 존재하지 않으면 EntityNotFoundException이 발생할 수 있다.
            - 조회한 ID에 해당하는 데이터가 존재하지 않더라도 프록시 객체는 먼저 반환될 수 있다.
            - 프록시를 초기화하려면 영속성 컨텍스트가 필요하다. 따라서 트랜잭션이 종료된 후 초기화되지 않은 프록시의 필드에 접근하면
                LazyInitializationException이 발생할 수 있다.
            - 엔티티의 실제 정보는 필요하지 않고, 다른 엔티티와의 연관관계를 설정하기 위해 ID만 필요한 경우 사용되어진다.
    - findALL(..): 모든 엔티티를 조회한다. 정렬(Sort)이나 페이징(Pageable) 조건을 파라미터로 제공할 수 있다.
*/
public interface MemberRepository extends JpaRepository<Member, Long> {

    /*
        스프링 데이터 JPA가 제공하는 쿼리 메서드 기능 3가지
        - 메서드 이름으로 쿼리 생성
        - 메서드 이름으로 JPA NamedQuery 호출
        - @Query 애노테이션을 사용해서 리포지토리 인터페이스에 쿼리 직접 정의
    */

    /*
        메서드 이름으로 쿼리 생성
        - 조회: find…By, read…By, query…By get…By
            - 예:) findHelloBy 처럼 ...에 식별하기 위한 내용(설명)이 들어가도 된다.
        - COUNT: count…By 반환타입 long
        - EXISTS: exists…By 반환타입 boolean
        - 삭제: delete…By, remove…By 반환타입 long
        - DISTINCT: findDistinct, findMemberDistinctBy
        - LIMIT: findFirst3, findFirst, findTop, findTop3
    */
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
    List<Member> findHelloBy(); // Member 전체 조회 쿼리
    List<Member> findTop3HelloBy();

    /*  메서드 이름으로 JPA NamedQuery 호출
        @Query(name = "Member.findByUsername") 생략 가능
        - 스프링 데이터 JPA는 선언한 "도메인 클래스(JpaRepository<Member, Long>로 선언한 Member) + .(점) +
            메서드 이름(지금 작성한 메서드 이름)"으로 Named 쿼리를 찾아서 실행
        - 만약 실행할 Named 쿼리가 없으면 메서드 이름으로 쿼리 생성 전략을 사용한다.
        - 참고: 스프링 데이터 JPA를 사용하면 실무에서 Named Query를 직접 등록해서 사용하는 일은 드물다.
            대신 @Query를 사용해서 리파지토리 메서드에 쿼리를 직접 정의한다.
    */
//    @Query(name = "Member.findByUsername")
    List<Member> findByUsername(@Param("username") String username);

    // @Query 애노테이션을 사용해서 리포지토리 인터페이스에 쿼리 직접 정의
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findMember(@Param("username") String username, @Param("age") int age);

    @Query("select m.username from Member m")
    List<String> findUsernameList();

    @Query("select new study.data_jpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);

    /*
        반환 타입
        - 스프링 데이터 JPA는 유연한 반환 타입 지원
        - 조회 결과가 많거나 없으면?
            - 컬렉션
                결과 없음: 빈 컬렉션 반환
            - 단건 조회
                결과 없음: null 반환
                결과가 2건 이상: jakarta.persistence.NonUniqueResultException 예외 발생
    */
    List<Member> findListByUsername(String username); // 컬렉션
    /*
        단건으로 지정한 메서드를 호출하면 스프링이 데이터 JPA는 내부에서 JPQL의 Query.getSingleResult() 메서드를 호출함
        이 메서드를 호출했을 때 조회 결과가 없으면 jakarta.persistence.NoResultException 예외가 발생하는데
        개발자 입장에서는 다루기가 상당히 불편하므로 스프링 데이터 JPA는 단건을 조회할 때 이 예외가 발생하면 해당 예외를 try-catch로 잡아서
        null을 반환해준다.
    */
    Member findMemberByUsername(String username); // 단건
    Optional<Member> findOptionalByUsername(String username); // 단건 Optional

    /*
        페이징과 정렬 파라미터
        - org.springframework.data.domain.Sort: 정렬 기능
        - org.springframework.data.domain.Pageable: 페이징 기능 (내부에 'Sort' 포함)

        특별한 반환 타입
        - org.springframework.data.domain.Page: 추가 count 쿼리 결과를 포함하는 페이징
        - org.springframework.data.domain.Slice: 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적으로 limit + 1조회)
            - 모바일 화면에서 상품을 10개씩 보여주고, 패이지 번호 대신 '더보기' 버튼이나 무한 스크롤로 다음 상품을 조회할 때 사용한다.
            - 요청한 10개보다 1개를 추가로 조회하여 다음 데이터의 존재 여부를 확인하고, 다음 상품이 존재하면 '더보기' 버튼을 활성화한다.
                사용자가 버튼을 누르면 다음 페이지의 상품을 다시 10개 조회한다.
            - 전체 데이터 개수와 전체 페이지 수가 필요하지 않을 경우에 사용하면 유용한 기능이다.
        - List(자바 컬렉션): 추가 count 쿼리 없이 결과만 반환
    */
    Page<Member> findByAge(int age, Pageable pageable);
    Slice<Member> findSliceByAge(int age, Pageable pageable);
    /*
        페이징 조회에서는 실제 데이터를 조회하는 쿼리뿐만 아니라 전체 데이터 개수를 구하는 쿼리의 성능도 중요하다.
        데이터 조회 쿼리는 limit을 사용하여 필요한 개수만 반환하지만, count 쿼리는 조건에 해당하는 전체 데이터를 확인해야 하므로
            데이터가 많아질수록 성능 저하가 발생할 수 있다.
        특히 조회 쿼리에 여러 join이 포함되어 있으면 자동으로 생성되는 count 쿼리에도 불필요한 join이 포함될 수 있다.
        연관 데이터가 조회 결과를 구성하기 위해서만 필요하고, 검색 조건이나 전체 개수에는 영향을 주지 않는다면 count 쿼리에는 해당 Join을 제거할 수 있다.
        따라서 개발자는 join을 제거해도 전체 개수가 동일한지 판단한 뒤, 더 단순한 count 쿼리를 별도로 작성하여 페이징 성능을 최적화해야 한다.
        단, 조인된 테이블의 컬럼이 검색 조건에 사용되거나 join이 결과 개수에 영향을 준다면 count 쿼리에서도 해당 join을 제거하면 안 된다.
    */
    @Query(value = "select m, t from Member m left join m.team t where m.age = :age",
            countQuery = "select count(m) from Member m")
    Page<Member> findCountQueryByAge(int age, Pageable pageable);

    /*
        @Modifying
        - Spring Data JPA에서 @Query로 작성한 쿼리가 SELECT 조회 쿼리가 아니라 UPDATE / DELETE와 같이 DB 데이터를 변경하는 쿼리임을
            Spring에게 알려주는 애노테이션
            @Query: 실행할 JPQL을 정의
            @Modifying: 해당 JPQL이 조회가 아닌 변경 쿼리임을 지정
        - 주의: UPDATE / DELETE 벌크 연산은 영속성 컨텍스트의 엔티티를 하나씩 변경하는 것이 아니라 DB에 직접 쿼리를 실행
            따라서 벌크 연산 이후 영속성 컨텍스트의 값과 DB의 값이 다른 상태가 발생할 수 있음
            이 경우 @Modifying의 clearAutomatically = true 속성을 사용하면 변경 쿼리 실행 후 영속성 컨텍스트를 자동으로 clear할 수 있다.

        @Modifying 쿼리의 트랜잭션
        - UPDATE / DELETE 같은 변경 쿼리는 실행 시점에 활성 트랜잭션이 필요하다.
        - 트랜잭션은 반드시 Repository 메서드 자체에 @Transactional을 선언해야 하는 것은 아니다.
        - 상위 Service 메서드에서 이미 @Transactional로 트랜잭션을 시작했다면 Repository의 @Modifying 메서드는 해당 트랜잭션 안에서 실행될 수 있다.
        - Service에도 @Transactional이 없고 Repository 메서드에도 @Transactional이 없다면 변경 쿼리를 실행할 활성 트랜잭션이 없으므로
            TransactionRequiredException 등의 문제가 발생할 수 있다.
        - @Query 또한 JPA의 JPQL이고, JPQL로 실제 DB 데이터를 변경하려면 트랜잭션 내부에서 실행되어야 한다.
            그런데 트랜잭션이 없으면 DB 변경 작업을 정상적으로 수행할 수 없기 때문에 예외가 발생한다.
     */

    @Modifying(clearAutomatically = true)
    @Query(value = "update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    /*
        [EntityGraph]
        - EntityGraph는 JPA 표준에서 제공하는 Fetch Plan 기능이다.
            LAZY로 설정된 연관관계라도 특정 조회에서 함께 로딩할 연관관계를 지정할 수 있다.
            Fetch Plan: 이번 쿼리에서 어디까지 데이터를 가져올지 정하는 로딩 계획

        - 다만 현재 Repository에서 사용하는 @EntityGraph 애노테이션 자체는 Spring Data JPA가
            JPA의 EntityGraph 기능을 편리하게 사용할 수 있도록 제공하는 애노테이션이다.

        EntityGraph 개념 및 @NamedEntityGraph → JPA 표준
        Repository의 @EntityGraph           → Spring Data JPA 편의 기능


        [attributePaths]
        @EntityGraph(attributePaths = "team")
        - Member를 조회할 때 team도 함께 조회하도록 Fetch Plan을 변경한다.
        - Member.team이 LAZY로 설정되어 있어도 해당 조회에서는 team까지 함께 로딩할 수 있다.


        [EntityGraph와 LEFT OUTER JOIN]
        - EntityGraph는 JOIN 종류를 직접 지정하는 기능은 아니다.
            어떤 연관관계를 함께 로딩할 것인지를 지정하는 Fetch Plan이다.

        - 다만 Hibernate가 EntityGraph를 실제 SQL로 구현할 때 LEFT OUTER JOIN 형태의 SQL을 생성하는 경우가 많다.

        - EntityGraph의 목적은 연관관계를 함께 조회하는 것이지 연관 엔티티가 존재하지 않는 원본 엔티티를 조회 결과에서 제외하는 것이 아니다.

          예를 들어 Member.team이 null일 수 있다고 가정하면

          Member1 -> TeamA
          Member2 -> null

          Member 전체를 조회하면서 team을 EntityGraph로 가져오려고 할 때
            INNER JOIN을 사용하면 Team이 없는 Member2가 조회 결과에서 사라질 수 있다.

          INNER JOIN
          Member1 -> 조회 O
          Member2 -> 조회 X

          LEFT OUTER JOIN
          Member1 -> 조회 O, TeamA 함께 조회
          Member2 -> 조회 O, Team은 null

        - 따라서 원래 조회 대상인 Member는 그대로 유지하면서 존재하는
            Team만 함께 로딩하기 위해 Hibernate가 LEFT OUTER JOIN으로 구현하는 경우가 많다.

        - 단, EntityGraph = LEFT JOIN이라고 정의된 것은 아니다.
              EntityGraph는 Fetch Plan을 지정하는 JPA 기능이고, 실제 SQL의 JOIN 방식은
                JPA 구현체(Hibernate)의 처리 방식과 연관관계 매핑 등에 따라 달라질 수 있다.


        [JPQL Fetch Join과 차이]
        - Fetch Join은 JPQL에서 JOIN 종류와 조회 구조를 직접 작성한다.

          select m from Member m join fetch m.team
          → INNER JOIN FETCH

          select m from Member m left join fetch m.team
          → LEFT OUTER JOIN FETCH

        - 따라서 JPQL Fetch Join에서는 join fetch와 left join fetch의 의미가 명확하게 다르다.

        - EntityGraph는 쿼리 자체에서 JOIN 구조를 작성하는 것이 아니라 기존 조회 쿼리에
            이 연관관계도 함께 가져와라라는 Fetch Plan을 추가한다.


        [사용 기준]
        - 단순 조회에서 특정 연관관계만 함께 가져오고 싶다면 @EntityGraph를 사용하면 JPQL을 직접 작성하지 않아도 되어 편리하다.

        - 조건이 복잡하거나 JOIN의 종류를 직접 선택해야 하거나, 여러 JOIN과 WHERE 조건을 세밀하게 제어해야 한다면
            JPQL + Fetch Join을 직접 작성하는 편이 명확하다.

        정리
        단순 Fetch Plan 변경
        → @EntityGraph

        복잡한 조회 / JOIN 구조 직접 제어
        → JPQL + Fetch Join
    */
    /*
        Spring Data JPA가 기본으로 제공하는 findAll() 같은 메서드도 Repository 인터페이스에서 다시 선언한 뒤 @EntityGraph를 적용할 수 있다.
        즉 findAll()의 조회 로직을 직접 새로 구현하는 것이 아니라, Spring Data JPA가 제공하는 기존 메서드를 그대로 사용하면서
            해당 조회에만 특정 연관관계를 함께 로딩하도록 설정할 수 있다.
    */
    @Override
    @EntityGraph(attributePaths = ("team"))
    List<Member> findAll();

    @EntityGraph(attributePaths = ("team"))
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

//    @EntityGraph(attributePaths = ("team"))
    @EntityGraph("Member.all")
    List<Member> findEntityGraphByUsername(@Param("username") String username);
}
