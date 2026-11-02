package com.psyweb.auth.authorization;

public interface CurrentActorProvider {
	CurrentActor requireCurrentActor();
}