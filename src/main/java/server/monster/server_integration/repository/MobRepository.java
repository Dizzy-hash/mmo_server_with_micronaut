package server.monster.server_integration.repository;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import server.common.dto.Location;
import server.common.mongo.query.MongoDbQueryHelper;
import server.configuration.MongoConfiguration;
import server.monster.server_integration.model.Monster;

@Slf4j
@Singleton
@Repository
public class MobRepository {

    MongoConfiguration configuration;
    MongoClient mongoClient;
    MongoCollection<Monster> mobMotionMongoCollection;

    public MobRepository(MongoConfiguration configuration, MongoClient mongoClient) {
        this.configuration = configuration;
        this.mongoClient = mongoClient;
        prepareCollections();
    }

    public Single<Monster> findMobInstance(String mobInstanceId) {
        return Single.fromPublisher(
                mobMotionMongoCollection.find(eq("mobInstanceId", mobInstanceId)));
    }

    public Single<Monster> findMobById(String mobId) {
        return Single.fromPublisher(
                mobMotionMongoCollection.find(eq("mobId", mobId)));
    }

    public Single<Monster> insertMobInstance(Monster mobInstance) {
        return Single.fromPublisher(mobMotionMongoCollection.insertOne(mobInstance))
                .map(success -> mobInstance);
    }

    public Single<Monster> updateMobMotion(Monster mobMotion) {
        return Single.fromPublisher(
                mobMotionMongoCollection.findOneAndReplace(
                        eq("mobInstanceId", mobMotion.getMobInstanceId()), mobMotion));
    }

    public Single<Monster> updateMotionOnly(Monster mobMotion) {
        return Single.fromPublisher(
                mobMotionMongoCollection.findOneAndUpdate(
                        eq("mobInstanceId", mobMotion.getMobInstanceId()),
                        Updates.set("motion", mobMotion.getMotion())));
    }

    public Single<DeleteResult> deleteMobInstance(String mobInstanceId) {
        return Single.fromPublisher(
                mobMotionMongoCollection.deleteOne(eq("mobInstanceId", mobInstanceId)));
    }

    public Single<List<Monster>> getMobsNearby(Location location) {
        return MongoDbQueryHelper.nearbyMobMotionFinder(mobMotionMongoCollection, location, 1000);
    }

    private void prepareCollections() {
        this.mobMotionMongoCollection =
                mongoClient
                        .getDatabase(configuration.getDatabaseName())
                        .getCollection(configuration.getMobInstance(), Monster.class);
    }
}
