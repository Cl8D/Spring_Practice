package jpabook.jpashop3.repository;

import jpabook.jpashop3.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private final EntityManager em;

    // 주문 저장
    public void save (Order order) {
        em.persist(order);
    }

    // 주문 한 개 조회
    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    // 주문 조회 - 검색 이용
    public List<Order> findAllByString(OrderSearch orderSearch) {
        // 주문자와 상태가 동일한 기존 주문 검색하기

        // jpql을 이용한 동적 쿼리
        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    // fetch join 활용한 코드
    public List<Order> findAllWithMemberDelivery() {
        // order를 가져올 때 member, delivery까지 한 번에 가져오기
        // 1번의 select절로 한 번에 가져오는 것.
        // 이때 proxy 객체를 가져오는 것이 아니라,
        // 그냥 join을 통해서 실제 db에 있는 애들을 한 번에 가져온다구 생각하자
        String query = "select o from Order o"
                + " join fetch o.member m"
                + " join fetch o.delivery d";

        return em.createQuery(query, Order.class).getResultList();
    }
}
