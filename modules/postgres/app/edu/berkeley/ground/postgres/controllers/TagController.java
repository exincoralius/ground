/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.berkeley.ground.postgres.controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorSystem;
import edu.berkeley.ground.lib.exception.GroundException;
import edu.berkeley.ground.lib.model.version.Tag;
import edu.berkeley.ground.postgres.dao.TagDao;
import edu.berkeley.ground.postgres.utils.GroundUtils;
import edu.berkeley.ground.postgres.utils.IdGenerator;
import edu.berkeley.ground.postgres.utils.PostgresUtils;
import play.cache.CacheApi;
import play.db.Database;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

// TODO delete this when done. this is just for testing
public class TagController extends Controller {
  private CacheApi cache;
  private Database dbSource;
  private ActorSystem actorSystem;
  private IdGenerator idGenerator;

  @Inject
  final void injectUtils(final CacheApi cache, final Database dbSource, final ActorSystem actorSystem, final IdGenerator idGenerator) {
    this.dbSource = dbSource;
    this.actorSystem = actorSystem;
    this.cache = cache;
    this.idGenerator = idGenerator;
  }

  public final CompletionStage<Result> getTag(long id) {
    CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
      String sql = String.format("select * from item_tag where item_id = %d", id);
      try {
        return cache.getOrElse("item_tag", () -> PostgresUtils.executeQueryToJson(dbSource, sql),
          Integer.parseInt(System.getProperty("ground.cache.expire.secs")));
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, PostgresUtils.getDbSourceHttpContext(actorSystem)).thenApply(output -> ok(output)).exceptionally(e -> {
      return internalServerError(GroundUtils.getServerError(request(), e));
    });
    return results;
  }

  @BodyParser.Of(BodyParser.Json.class)
  public final CompletionStage<Result> createTag() {
    CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
      JsonNode json = request().body().asJson();
      Tag tag = Json.fromJson(json, Tag.class);
      try {
        new TagDao().create(dbSource, tag, idGenerator);
      } catch (GroundException e) {
        throw new CompletionException(e);
      }
      return String.format("New Tag Created Successfully");
    }, PostgresUtils.getDbSourceHttpContext(actorSystem)).thenApply(output -> created(output)).exceptionally(e -> {
      if (e.getCause() instanceof GroundException) {
        return badRequest(GroundUtils.getClientError(request(), e, GroundException.exceptionType.ITEM_NOT_FOUND));
      } else {
        return internalServerError(GroundUtils.getServerError(request(), e));
      }
    });
    return results;
  }

}
