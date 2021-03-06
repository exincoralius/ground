package edu.berkeley.ground.postgres.dao;

import com.fasterxml.jackson.databind.JsonNode;
import edu.berkeley.ground.lib.exception.GroundException;
import edu.berkeley.ground.lib.factory.core.NodeVersionFactory;
import edu.berkeley.ground.lib.model.core.NodeVersion;
import edu.berkeley.ground.lib.model.version.Tag;
import edu.berkeley.ground.postgres.utils.IdGenerator;
import edu.berkeley.ground.postgres.utils.PostgresUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import play.db.ConnectionCallable;
import play.db.ConnectionRunnable;
import play.db.Database;
import play.libs.Json;

import javax.sql.DataSource;

public class NodeVersionDao implements NodeVersionFactory {

  public final void insertIntoDatabase(final Database dbSource, final NodeVersion nodeVersion, IdGenerator idGenerator)
      throws GroundException {
    final List<String> sqlList = new ArrayList<>();
    final long uniqueId = idGenerator.generateVersionId();

    //Call super to create 1.version, 2. structure version (need to create a node_id)?, 3. rich version, 4. node_version
    try {
      sqlList.add(
        String.format(
          "insert into version (id) values (%s)", uniqueId)
      );
      sqlList.add(
        String.format("insert into structure_version (id, structure_id) values (%s,%s)",
          uniqueId, nodeVersion.getStructureVersionId())
      );
      sqlList.add(
        String.format("insert into rich_version (id, structure_version_id, reference) values (%s,%s,\'%s\')",
          uniqueId, nodeVersion.getStructureVersionId(), nodeVersion.getReference())
      );
      sqlList.add(
        String.format(
          "insert into node_version (id, node_id) values (%s,%s)",
          uniqueId, nodeVersion.getNodeId()));
      PostgresUtils.executeSqlList(dbSource, sqlList);
    } catch (Exception e) {
      throw new GroundException(e);
    }
  }

  @Override
  public void insertIntoDatabase(long id) {

  }

  @Override
  public void insertIntoDatabase(long id, Map<String, Tag> tags, long structureVersionId, String reference, Map<String, String> referenceParameters) throws GroundException {

  }

  @Override
  public NodeVersion retrieveFromDatabase(long id) throws GroundException {
    return null;
  }

  @Override
  public NodeVersion retrieveFromDatabase(Database dbSource, long id) throws GroundException {
    final List<String> sqlList = new ArrayList<String>();
    String sql = String.format("select * from node_version where id=%d", id);
    JsonNode json = Json.parse(PostgresUtils.executeQueryToJson(dbSource, sql));
    return Json.fromJson(json, NodeVersion.class);
  }
}
