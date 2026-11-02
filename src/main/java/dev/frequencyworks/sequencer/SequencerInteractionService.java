package dev.frequencyworks.sequencer;

import com.mojang.authlib.GameProfile;
import dev.frequencyworks.sequencer.SequencerProgram.Link;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Executes precisely one explicitly linked block-face action per instruction.
 * Uses its own stable fake-player profile rather than another mod's shared
 * fake player. The normal server game mode handles interaction/break events
 * and protection checks. No radius scanning or forced chunk loads.
 */
public final class SequencerInteractionService {
    private SequencerInteractionService(){}

    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("FrequencyWorks:Sequencer".getBytes(StandardCharsets.UTF_8)),
            "[FW Sequencer]");

    public enum Outcome { SUCCESS, BLOCKED }

    /** 0 right-click, 1 sneak-right-click, 2 punch, 3 mine. */
    public static Outcome perform(ServerLevel level, Link link, int action) {
        BlockPos target=link.pos();
        if (!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)
                || level.getBlockState(target).isAir()) return Outcome.BLOCKED;

        var fake = FakePlayerFactory.get(level,PROFILE);
        if (!level.mayInteract(fake,target)) return Outcome.BLOCKED;

        // Position the fake actor directly outside the selected face. The
        // player interactions run on the authoritative server, not the client.
        Vec3 hit=Vec3.atCenterOf(target).add(
                link.face().getStepX()*0.5,
                link.face().getStepY()*0.5,
                link.face().getStepZ()*0.5);
        fake.setPos(hit.x + link.face().getStepX(),
                hit.y + link.face().getStepY(),
                hit.z + link.face().getStepZ());
        fake.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        fake.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        fake.setShiftKeyDown(action==1);
        try {
            return switch(Math.floorMod(action,4)){
                case 0,1 -> {
                    var response=fake.gameMode.useItemOn(fake,level,ItemStack.EMPTY,
                            InteractionHand.MAIN_HAND,new BlockHitResult(hit,link.face(),target,false));
                    yield response.consumesAction()?Outcome.SUCCESS:Outcome.BLOCKED;
                }
                case 2 -> {
                    // Punch invokes a single left-click hook without digging or
                    // breaking the block. Respect cancel/deny from protection
                    // mods; mining is a distinct explicit action.
                    var event = new PlayerInteractEvent.LeftClickBlock(fake,target,link.face(),
                            PlayerInteractEvent.LeftClickBlock.Action.START);
                    NeoForge.EVENT_BUS.post(event);
                    if(event.isCanceled() || event.getUseBlock()==TriState.FALSE) yield Outcome.BLOCKED;
                    level.getBlockState(target).attack(level,target,fake);
                    yield Outcome.SUCCESS;
                }
                default -> {
                    // Mine is a requested destructive action. Respect
                    // unbreakable hardness and the normal fake-player break
                    // event/protection pipeline.
                    if(level.getBlockState(target).getDestroySpeed(level,target)<0){
                        yield Outcome.BLOCKED;
                    }
                    yield fake.gameMode.destroyBlock(target)?Outcome.SUCCESS:Outcome.BLOCKED;
                }
            };
        } finally {
            fake.setShiftKeyDown(false);
            fake.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            fake.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }
    }
}
