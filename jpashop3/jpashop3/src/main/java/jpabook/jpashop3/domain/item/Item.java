package jpabook.jpashop3.domain.item;

import jpabook.jpashop3.domain.Category;
import jpabook.jpashop3.exception.NotEnoughStockException;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

// 엔티티에 적용할 때는 이런 식으로
// @BatchSize(size=1000)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Getter @Setter
@DiscriminatorColumn(name="dtype")
public abstract class Item {

    @Id @GeneratedValue
    @Column(name="item_id")
    private Long id;
    private String name;
    private int Price;
    private int stockQuantity;

    // item(n) - category(n)
    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();

    // 재고 수량 증가
    public void addStock (int quantity) {
        this.stockQuantity += quantity;
    }

    // 재고 수량 감소
    public void removeStock (int quantity) {
        int restStock = this.stockQuantity - quantity;

        // 재고 수량이 0보다 작아지는 경우 체크
        if (restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }
}
