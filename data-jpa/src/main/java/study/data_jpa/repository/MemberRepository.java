package study.data_jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
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
        연간 데이터가 조회 결과를 구성하기 위해서만 필요하고, 검색 조건이나 전체 개수에는 영향을 주지 않는다면 count 쿼리에는 해당 Join을 제거할 수 있다.
        따라서 개발자는 join을 제거해도 전체 개수가 동일한지 판단한 뒤, 더 단순한 count 쿼리를 별도로 작성하여 페이징 성능을 최적화해야 한다.
        단, 조인된 테이블의 컬럼이 검색 조건에 사용되거나 join이 결과 개수에 영향을 준다면 count 쿼리에서도 해당 join을 제거하면 안 된다.
    */
    @Query(value = "select m, t from Member m left join m.team t where m.age = :age",
            countQuery = "select count(m) from Member m")
    Page<Member> findCountQueryByAge(int age, Pageable pageable);

}
