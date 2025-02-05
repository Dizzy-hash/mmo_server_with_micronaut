package server.monster.server_integration.listener;

import io.micronaut.configuration.kafka.annotation.*;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import server.attribute.stats.service.StatsService;
import server.monster.server_integration.model.Monster;
import server.monster.server_integration.producer.MonsterServerProducer;
import server.monster.server_integration.service.MobInstanceService;

@Slf4j
@KafkaListener(
        groupId = "mmo-server",
        offsetReset = OffsetReset.EARLIEST,
        offsetStrategy = OffsetStrategy.SYNC,
        clientId = "mob_repo_client")
public class MonsterServerListener {

    @Inject MonsterServerProducer monsterServerProducer;

    @Inject MobInstanceService mobInstanceService;

    @Inject StatsService statsService;

    @Topic("create-mob")
    public void receiveCreateMob(Monster monster) {
        mobInstanceService
                .createMob(monster)
                .doOnSuccess(
                        mob -> statsService.initializeMobStats(monster.getMobInstanceId()))
                .doOnError(error -> log.error("Error on creating mob, {}", error.getMessage()))
                .subscribe();
    }

    @Topic("mob-motion-update")
    public void receiveUpdateMob(Monster monster) {
        // Add validation
        // TODO: Move these writes over to cache sync to db service
        mobInstanceService
                .updateMobMotion(monster.getMobInstanceId(), monster.getMotion())
                .doOnError(
                        error ->
                                log.error(
                                        "Error processing mob motion update, {}",
                                        error.getMessage()))
                .subscribe();
        monsterServerProducer.sendMobUpdateResult(monster);
    }
}
