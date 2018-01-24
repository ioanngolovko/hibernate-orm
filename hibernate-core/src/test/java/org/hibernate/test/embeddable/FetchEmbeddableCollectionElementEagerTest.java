package org.hibernate.test.embeddable;

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
public class FetchEmbeddableCollectionElementEagerTest extends BaseCoreFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[]{Chart.class, TreasureMap.class};
  }

  @Test
  public void testExplicitFetch() {
    doInHibernate(this::sessionFactory, session -> {
      Chart chart = new Chart();
      chart.points = Arrays.asList(new Point(1, 1), new Point(2, 2));
      session.save(chart);
    });

    List<Chart> charts = new ArrayList<>();
    doInHibernate(this::sessionFactory, session -> {
      NativeQuery<Chart> query = session.createNativeQuery(
          "SELECT * FROM charts c" +
              " JOIN chart_points cp ON c.id = cp.chart_id"
      );
      query.addRoot("c", Chart.class);
      query.addFetch("cp", "c", "points");
      query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
      List<Chart> queryResult = query.getResultList();

      charts.addAll(queryResult);
    });
    Assert.assertEquals(1, charts.size());
    Assert.assertTrue(Hibernate.isInitialized(charts.get(0).points));
    Assert.assertEquals(2, charts.get(0).points.size());
  }

  @Test
  public void testImplicitFetch() {
    doInHibernate(this::sessionFactory, session -> {
      TreasureMap treasureMap = new TreasureMap();
      treasureMap.points = Arrays.asList(new Point(1, 1), new Point(2, 2));
      session.save(treasureMap);
    });

    List<TreasureMap> treasureMaps = new ArrayList<>();
    doInHibernate(this::sessionFactory, session -> {
      NativeQuery<TreasureMap> query = session.createNativeQuery(
          "SELECT * FROM treasure_maps", TreasureMap.class);
      List<TreasureMap> queryResult = query.getResultList();
      treasureMaps.addAll(queryResult);
    });

    Assert.assertEquals(1, treasureMaps.size());
    Assert.assertTrue(Hibernate.isInitialized(treasureMaps.get(0).points));
    Assert.assertEquals(2, treasureMaps.get(0).points.size());
  }

  /**
   * @author Ivan Golovko
   */
  @Entity
  @Table(name = "charts")
  public static class Chart {

    @Id
    @GeneratedValue
    Integer id;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "chart_points",
        joinColumns=@JoinColumn(name = "chart_id")
    )
    List<Point> points;
  }

  /**
   * @author Ivan Golovko
   */
  @Entity
  @Table(name = "treasure_maps")
  public static class TreasureMap {

    @Id
    @GeneratedValue
    Integer id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "treasure_map__points",
        joinColumns=@JoinColumn(name = "treasure_map_id")
    )
    List<Point> points;
  }

  /**
   * @author Ivan Golovko
   */
  @Embeddable
  public static class Point {
    Integer x;
    Integer y;

    public Point() {}

    public Point(Integer x, Integer y) {
      this.x = x;
      this.y = y;
    }
  }
}
