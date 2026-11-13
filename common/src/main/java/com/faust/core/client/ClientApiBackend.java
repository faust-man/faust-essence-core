package com.faust.core.client;
import com.faust.essence.api.*;
import com.faust.essence.api.client.ClientBackend;
import java.util.Optional;
public final class ClientApiBackend implements ClientBackend {
 public Optional<PlayerSnapshot> snapshot(){return CoreClient.progress==null||CoreClient.balance==null?Optional.empty():Optional.of(com.faust.core.runtime.EssenceBackendImpl.view(CoreClient.progress,CoreClient.balance));}
 public Optional<ResourceProvider.ResourceState> stamina(){if(CoreClient.progress==null||CoreClient.balance==null||!com.faust.core.progression.StaminaServices.present())return Optional.empty();var p=CoreClient.progress;return Optional.of(new ResourceProvider.ResourceState(p.stamina,p.maxStamina(CoreClient.balance),p.fatigue,p.actionsBlocked(CoreClient.balance)));}
 public boolean resourceEnabled(){return CoreClient.balance!=null&&CoreClient.balance.stamina.get("enabled")>0;}
}
