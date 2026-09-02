package study.data_jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import study.data_jpa.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;

    public Member save(Member member) {
        em.persist(member);
        return member;
    }

    public void delete(Member member) {
        em.remove(member);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    /*
        Optional<T>: Optional은 Java 8에서 추가된 클래스로, 값이 존재할 수도 있고 존재하지 않을 수도 있다는 것을 표현하는 컨테이너이다.
        - 기존에는 조회 결과가 없을 때 null을 반환했지만, null을 그대로 사용하면 NullPointerException이 발생할 수 있고 메서드 선언만으로는 null 반환 가능성을 알기 어렵다.
        - Optional을 반환하면 호출하는 쪽에서 값이 없을 가능성을 명확하게 확인하고 처리할 수 있다.
        [Optional 생성]
        - Optional.of(value)
            - 반드시 null이 아닌 값을 감쌀 때 사용한다.
            - value가 null이면 NullPointerException이 발생한다.
        - Optional.ofNullable(value)
            - 값이 null일 수도 있을 때 사용한다.
            - 값이 있으면 해당 값을 포함한 Optional을 반환하고,
            - null이면 Optional.empty()를 반환한다.
        - Optional.empty()
            - 값이 없는 빈 Optional을 생성한다.
        [주요 메서드]
        - isPresent()
            - 값이 존재하면 true, 없으면 false를 반환한다.
        - get()
            - 값을 꺼낸다.
            - 값이 없으면 NoSuchElementException이 발생하므로 값의 존재 여부를 확인하지 않고 사용하는 것은 피하는 것이 좋다.
        - ifPresent(action)
            - 값이 있을 때만 전달한 작업을 실행한다.
        - orElse(defaultValue)
            - 값이 있으면 해당 값을 반환하고, 없으면 지정한 기본값을 반환한다.
        - orElseGet(supplier)
            - 값이 있으면 해당 값을 반환하고, 없을 때만 기본값을 생성하는 로직을 실행한다.
        - orElseThrow(exceptionSupplier)
            - 값이 있으면 해당 값을 반환하고, 없으면 지정한 예외를 발생시킨다.
        - map(function)
            - Optional 안에 값이 있을 때만 해당 객체의 메서드를 호출한다.
            - 메서드의 반환값을 다시 Optional로 감싸서 다음 map()에 전달한다.
            예)
                map(Member::getAddress)
                - Optional<Member> 안의 Member에서 Address를 꺼낸다.
                - 결과: Optional<Address>
                map(Address::getCity)
                - Optional<Address> 안의 Address에서 도시 이름을 꺼낸다.
                - 결과: Optional<String>
                회원, 주소 또는 도시 이름 중 하나라도 값이 존재하지 않는면 Optional.empty가 된다.
        - filter(조건)
            - Optional 안에 값이 있을 때 해당 값이 조건을 만족하는지 검사한다.
            - 조건을 만족하면 원래 값을 그대로 유지한다.
            - 조건을 만족하지 않으면 값을 제거하고 빈 Optional을 반환한다.
            예)
                Optional.of(20).filter(age -> age >= 19)
                    -> 조건을 만족하므로 Optional[20]
                Optional.of(20).filter(age -> age >= 30)
                    -> 조건을 만족하지 않으므로 Optional.empty
        - flatMap(변환 작업)
            - Optional 안의 값으로 다른 메서드를 호출할 때 사용한다.
            - 이때 호출하는 메서드의 반환 타입이 이미 Optional인 경우에 사용한다.
            - 반환된 Optional을 다시 optional로 감싸지 않는다.
            예)
                Optional<Address> findAddress()
                map() 사용:
                    optionalMember.map(Member::findAddress)
                    -> Optional<Optional<Address>>가 됨
                flatMap() 사용:
                    optionalMember.flatMap(Member::findAddress)
                    -> Optional<Address>가 됨

        [사용 시 주의사항]
        1. Optional은 주로 값이 없을 수 있는 메서드의 반환 타입으로 사용한다.
        2. Optional을 반환한다고 선언한 메서드에서는 null이 아니라 Optional.empty()를 반환해야 한다.
        3. get()을 무작정 사용하면 값이 없을 때 예외가 발생하므로 irElse(), orElseGet(), orElseThrow() 등을 사용하는 것이 좋다.
        4. List와 같은 컬렉션은 결과가 없으면 빈 컬렉션을 반환할 수 있으므로 일반적으로 Optional<List<T>> 형태로 감싸지 않는다.
        5. Optional은 NullPointerException을 완전히 막아주는 기능이 아니라, 값이 없을 가능성을 반환 타입에 명확하게 표현하고
            호출자가 해당 상황을 처리하도록 돕는 클래스이다.
    */
    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public long count() {
        return em.createQuery("select count(m) from Member m", Long.class)
                .getSingleResult();
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
        return em.createQuery("select m from Member m where m.username = :username and m.age > :age", Member.class)
                .setParameter("username", username)
                .setParameter("age", age)
                .getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createNamedQuery("Member.findByUsername", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByPage(int age, int offset, int limit) {
        return em.createQuery("select m from Member m where m.age = :age order by m.username desc", Member.class)
                .setParameter("age", age)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long totalCount(int age) {
        return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
                .setParameter("age", age)
                .getSingleResult();
    }
}
