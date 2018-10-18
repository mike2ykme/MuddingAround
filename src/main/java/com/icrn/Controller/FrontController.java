package com.icrn.Controller;

import com.icrn.model.EntityType;
import com.icrn.model.MudUser;
import com.icrn.service.StateHandler;
import io.reactivex.Maybe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
public class FrontController {
    private final StateHandler stateHandler;

    public Maybe<MudUser> maybeGetUser(String username, String password) {
        return Maybe.create(maybeEmitter -> {
            this.stateHandler.getEntityByName(username)
                    .filter(entity -> entity.getType() == EntityType.USER)
                    .map(entity -> (MudUser) entity)
                    .filter(mudUser -> mudUser.getPassword() == password)
                    .subscribe(mudUser ->{
                        log.info("maybeGetUser() was able to find user: " + mudUser.getName() + " for username: " + username);
                        maybeEmitter.onSuccess(mudUser);
                    },maybeEmitter::onError,() -> {
                        log.info("maybeGetUser() completed for user: " + username );
                        maybeEmitter.onComplete();
                    });
            maybeEmitter.onComplete();
        });
    }
}
