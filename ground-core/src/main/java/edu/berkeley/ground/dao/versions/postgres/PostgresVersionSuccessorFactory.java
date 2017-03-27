/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.ground.dao.versions.postgres;

import edu.berkeley.ground.model.versions.GroundType;
import edu.berkeley.ground.model.versions.Version;
import edu.berkeley.ground.model.versions.VersionSuccessor;
import edu.berkeley.ground.dao.versions.VersionSuccessorFactory;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.PostgresClient;
import edu.berkeley.ground.db.QueryResults;
import edu.berkeley.ground.exceptions.EmptyResultException;
import edu.berkeley.ground.exceptions.GroundException;
import edu.berkeley.ground.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class PostgresVersionSuccessorFactory extends VersionSuccessorFactory {
  private final PostgresClient dbClient;
  private final IdGenerator idGenerator;

  public PostgresVersionSuccessorFactory(PostgresClient dbClient, IdGenerator idGenerator) {
    this.dbClient = dbClient;
    this.idGenerator = idGenerator;
  }

  public <T extends Version> VersionSuccessor<T> create(long fromId, long toId) throws GroundException {
    List<DbDataContainer> insertions = new ArrayList<>();
    long dbId = idGenerator.generateSuccessorId();

    insertions.add(new DbDataContainer("id", GroundType.LONG, dbId));
    insertions.add(new DbDataContainer("from_version_id", GroundType.LONG, fromId));
    insertions.add(new DbDataContainer("to_version_id", GroundType.LONG, toId));

    this.dbClient.insert("version_successor", insertions);

    return VersionSuccessorFactory.construct(dbId, toId, fromId);
  }

  public <T extends Version> VersionSuccessor<T> retrieveFromDatabase(long dbId) throws GroundException {
    List<DbDataContainer> predicates = new ArrayList<>();
    predicates.add(new DbDataContainer("id", GroundType.LONG, dbId));

    QueryResults resultSet;
    try {
      resultSet = this.dbClient.equalitySelect("version_successor", DBClient.SELECT_STAR, predicates);
    } catch (EmptyResultException e) {
      throw new GroundException("No VersionSuccessor found with id " + dbId + ".");
    }

    long toId = resultSet.getLong(2);
    long fromId = resultSet.getLong(3);

    return VersionSuccessorFactory.construct(dbId, toId, fromId);
  }
}