package org.hibernate.test.basic;

import org.hibernate.Criteria;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Ivan Golovko
 */
@TestForIssue(jiraKey = "HHH-8248")
public class FetchBasicCollectionElementEagerTest extends BaseCoreFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[]{City.class};
  }

  @Test
  public void test() {
    doInHibernate(this::sessionFactory, session -> {
      City city = new City();
      city.streets = Arrays.asList("Good Street", "Bad Street");
      session.save(city);
    });

    List<City> cities = new ArrayList<>();
    doInHibernate(this::sessionFactory, session -> {
      NativeQuery<City> query = session.createNativeQuery(
          "SELECT * FROM cities c" +
              " JOIN city_streets cs ON c.id = cs.city_id"
      );
      query.addRoot("c", City.class);
      query.addFetch("cs", "c", "streets");
      query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
      List<City> queryResult = query.getResultList();
      cities.addAll(queryResult);
    });

    Assert.assertEquals(1, cities.size());
    Assert.assertEquals(2, cities.get(0).streets.size());
  }


  /**
   * @author Ivan Golovko
   */
  @Entity
  @Table(name = "cities")
  public static class City {

    @Id
    @GeneratedValue
    Integer id;

    @Basic
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "city_streets",
        joinColumns=@JoinColumn(name = "city_id")
    )
    List<String> streets;
  }

}
