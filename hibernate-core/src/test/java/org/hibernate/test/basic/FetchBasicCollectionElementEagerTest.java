package org.hibernate.test.basic;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
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
    return new Class[]{City.class, Village.class};
  }

  @Test
  public void testExplicitFetch() {
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
    Assert.assertTrue(Hibernate.isInitialized(cities.get(0).streets));
    Assert.assertEquals(2, cities.get(0).streets.size());
  }

  @Test
  public void testImplicitFetch() {
    doInHibernate(this::sessionFactory, session -> {
      Village village = new Village();
      village.streets = Arrays.asList("Good Street", "Bad Street");
      session.save(village);
    });

    List<Village> villages = new ArrayList<>();
    doInHibernate(this::sessionFactory, session -> {
      NativeQuery<Village> query = session.createNativeQuery(
          "SELECT * FROM villages", Village.class);
      List<Village> queryResult = query.getResultList();
      villages.addAll(queryResult);
    });

    Assert.assertEquals(1, villages.size());
    Assert.assertTrue(Hibernate.isInitialized(villages.get(0).streets));
    Assert.assertEquals(2, villages.get(0).streets.size());
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

  /**
   * @author Ivan Golovko
   */
  @Entity
  @Table(name = "villages")
  public static class Village {
    @Id
    @GeneratedValue
    Integer id;

    @Basic
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "village_streets",
        joinColumns=@JoinColumn(name = "village_id")
    )
    List<String> streets;
  }

}
