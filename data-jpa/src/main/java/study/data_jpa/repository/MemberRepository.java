package study.data_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import study.data_jpa.entity.Member;

import java.util.List;

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
        - @Query 애노테이션을 사용해서 레파지토리 인터페이스에 쿼리 직접 정의
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
}
