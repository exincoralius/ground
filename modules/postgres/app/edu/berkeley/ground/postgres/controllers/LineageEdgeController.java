package edu.berkeley.ground.postgres.controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorSystem;
import edu.berkeley.ground.lib.exception.GroundException;
import edu.berkeley.ground.lib.model.usage.LineageEdge;
import edu.berkeley.ground.lib.model.usage.LineageEdgeVersion;
import edu.berkeley.ground.postgres.dao.LineageEdgeDao;
import edu.berkeley.ground.postgres.dao.LineageEdgeVersionDao;
import edu.berkeley.ground.postgres.utils.GroundUtils;
import edu.berkeley.ground.postgres.utils.PostgresUtils;
import play.cache.CacheApi;
import play.db.Database;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class LineageEdgeController extends Controller {
    private CacheApi cache;
    private Database dbSource;
    private ActorSystem actorSystem;

    @Inject
    final void injectUtils(final CacheApi cache, final Database dbSource, final ActorSystem actorSystem) {
        this.dbSource = dbSource;
        this.actorSystem = actorSystem;
        this.cache = cache;
    }

    public final CompletionStage<Result> getLineageEdge(String sourceKey) {
        CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
            String sql = String.format("select * from lineage_edge where source_key = \'%s\'", sourceKey);
            try {
                return cache.getOrElse("graphs", () -> PostgresUtils.executeQueryToJson(dbSource, sql),
                        Integer.parseInt(System.getProperty("ground.cache.expire.secs")));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, PostgresUtils.getDbSourceHttpContext(actorSystem)).thenApply(output -> ok(output)).exceptionally(e -> {
            return internalServerError(GroundUtils.getServerError(request(), e));
        });
        return results;
    }

    public final CompletionStage<Result> getLineageEdgeVersion(Long id) {
        CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
            String sql = String.format("select * from lineage_edge_version where id = \'%d\'", id);
            try {
                return cache.getOrElse("nodes", () -> PostgresUtils.executeQueryToJson(dbSource, sql),
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
    public final CompletionStage<Result> createLineageEdge() {
        CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
            JsonNode json = request().body().asJson();
            System.out.println(json.toString());
            LineageEdge lineageEdge = Json.fromJson(json, LineageEdge.class);
            System.out.println(lineageEdge.toString());
            try {
                new LineageEdgeDao().create(dbSource, lineageEdge);
            } catch (GroundException e) {
                throw new CompletionException(e);
            }
            return String.format("New Lineage Edge Created Successfully");
        }, PostgresUtils.getDbSourceHttpContext(actorSystem)).thenApply(output -> created(output)).exceptionally(e -> {
            if (e.getCause() instanceof GroundException) {
                return badRequest(GroundUtils.getClientError(request(), e, GroundException.exceptionType.ITEM_NOT_FOUND));
            } else {
                return internalServerError(GroundUtils.getServerError(request(), e));
            }
        });
        return results;
    }

    public final CompletionStage<Result> createLineageEdgeVersion() {
        CompletableFuture<Result> results = CompletableFuture.supplyAsync(() -> {
            JsonNode json = request().body().asJson();
            LineageEdgeVersion lineageEdgeVersion = Json.fromJson(json, LineageEdgeVersion.class);
            try {
                new LineageEdgeVersionDao().create(dbSource, lineageEdgeVersion);
            } catch (GroundException e) {
                throw new CompletionException(e);
            }
            return String.format("New Lineage Edge Version Created Successfully");
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