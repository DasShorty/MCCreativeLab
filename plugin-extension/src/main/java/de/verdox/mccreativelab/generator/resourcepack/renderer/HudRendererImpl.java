package de.verdox.mccreativelab.generator.resourcepack.renderer;

import de.verdox.mccreativelab.generator.resourcepack.renderer.element.HudRenderer;
import de.verdox.mccreativelab.generator.resourcepack.types.CustomHud;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class HudRendererImpl extends Thread implements HudRenderer {
    private final Map<Player, PlayerHudRendererData> renderingDataCache = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<TickData> ticksToProcess = new LinkedBlockingQueue<>();
    private boolean isRunning = true;

    public HudRendererImpl() {
        super(null, null, "HudRendererThread");
    }

    @Override
    public ActiveHud getActiveHud(Player player, CustomHud customHud) {
        PlayerHudRendererData playerHudRendererData = getRendererData(player);
        return playerHudRendererData.getActiveHud(customHud);
    }

    @Override
    public ActiveHud getOrStartActiveHud(Player player, CustomHud customHud) {
        PlayerHudRendererData playerHudRendererData = getRendererData(player);
        return playerHudRendererData.getOrStartActiveHud(customHud);
    }

    public boolean stopActiveHud(Player player, CustomHud customHud) {
        return getRendererData(player).removeFromRendering(customHud);
    }

    @Override
    public void forceUpdate(Player player) {
        getRendererData(player).forceUpdate();
    }

    public void addTickToRenderQueue(List<Player> serverPlayers) {
        ticksToProcess.offer(new TickData(Set.copyOf(serverPlayers)));
    }

    private ActiveHud createActiveHud(Player player, CustomHud customHud) {
        return new ActiveHud(player, customHud);
    }

    private PlayerHudRendererData getRendererData(Player player) {
        return renderingDataCache.computeIfAbsent(player, player1 -> new PlayerHudRendererData(player));
    }

    @Override
    public void interrupt() {
        isRunning = false;
        super.interrupt();
    }

    @Override
    public void run() {
        while(isRunning){
            try {
                var list = ticksToProcess.take().serverPlayers;
                for (Player player : list) {
                    getRendererData(player).sendUpdate();
                }
                for (Player player : renderingDataCache.keySet()) {
                    if(!list.contains(player))
                        renderingDataCache.remove(player);
                }
            } catch (InterruptedException ignored) {}
        }
    }

    public record TickData(Set<Player> serverPlayers) {

    }
}