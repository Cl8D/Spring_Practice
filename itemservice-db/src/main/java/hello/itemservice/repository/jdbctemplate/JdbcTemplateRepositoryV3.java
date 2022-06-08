package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SimpleJdbcInsert
 */
@Repository
@Slf4j
public class JdbcTemplateRepositoryV3 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;
    private final SimpleJdbcInsert jdbcInsert;

    public JdbcTemplateRepositoryV3(DataSource dataSource) {
        // 순서가 아닌, 이름 기반 파라미터 바인딩을 진행해준다!
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                // 테이블명 지정
                .withTableName("item")
                // key를 새성하는 pk값 지정
                .usingGeneratedKeyColumns("id");
        // insert SQL에 사용할 컬럼 지정. 특정 값만 저장하고 싶을 때 사용
        // 생성 시점에 db 테이블의 메타 데이터를 조회하기 때문에, 어떤 컬럼이 있는지 확인이 가능해서 usingColumns를 생략할 수 있다!
//                .usingColumns("item_name", "price", "quantity"); // 생략 가능
    }

    @Override
    public Item save(Item item) {
        // 파라미터로 넘어온 item 객체를 분석해서 얘를 가지고 파라미터를 만들어준다.
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        // insertSQL 실행 및 key를 반환받는다
        Number key = jdbcInsert.executeAndReturnKey(param);
        item.setId(key.longValue());
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item " +
                "set item_name=:itemName, price=:price, quantity=:quantity "
                +"where id=:id";

        // 여기서는 addValue를 통해서 이름 기반으로 파라미터를 넘겨준다!
        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);
        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";
        try {
            // Map을 파라미터로 넘겨준다. key를 "id"로, value로 각각의 id를 넘겨준다.
            Map<String, Object> param = Map.of("id", id);
            // 데이터 단건 조회 시 사용한다. - 여기서는 두 번째에 파라미터가 들어간다.
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            // 결과가 없을 때는 예외 발생
            // 둘 이상일 경우에는 IncorrectResultSizeDataAccessException
            // 결과가 없을 때 Optional 반환하기 때문에, 결과가 없으면 empty() 대신 반환해주기
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";

        // 동적 쿼리
        if(StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;

        // itemName이 있으면
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;
        }

        // maxPrice가 null이 아니면
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }

        log.info("sql={}", sql);
        // 결과가 하나 이상일 때는 .query()를 사용한다
        // 결과가 없을 때는 빈 컬렉션을 반환해준다.
        // 마찬가지로 여기서는 두 번째 인자로 param이 들어간다.
        // param에 있는 값을 꺼내서 위에 채워준다.
        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        // ResultSet의 결과를 Item 객체의 필드명에 따라서 알아서 넣어준다!
        return BeanPropertyRowMapper.newInstance(Item.class);
    }
}
