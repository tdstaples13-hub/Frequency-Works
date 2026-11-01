package dev.frequencyworks.tests;
import dev.frequencyworks.world.BufferInventoryActions;
import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public class BufferGuiChecks {
 private static void expect(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args){
   ServerPlayer player=new ServerPlayer(); StoredFrequency network=new StoredFrequency();
   player.getInventory().setItem(0,new ItemStack("iron",64,64));
   expect(BufferInventoryActions.deposit(player,network,0,1)==1,"one item deposit");
   expect(player.getInventory().getItem(0).getCount()==63,"source reduced by exactly 1");
   expect(BufferInventoryActions.deposit(player,network,0,64)==63,"stack deposit");
   expect(player.getInventory().getItem(0).isEmpty(),"source became empty");
   long id=network.seed(new ItemStack("gold",1,64),160);
   expect(BufferInventoryActions.withdraw(player,network,id,64)==64,"withdraw stack");
   expect(player.getInventory().getItem(0).getCount()==64,"player received gold");
   expect(network.findEntry(id).get().quantity()==96,"frequency debited 64");
   for(int i=1;i<36;i++)player.getInventory().setItem(i,new ItemStack("cobble",64,64));
   expect(BufferInventoryActions.withdraw(player,network,id,64)==0,"full inventory rejects withdrawal");
   expect(network.findEntry(id).get().quantity()==96,"no items debited on full inventory");
   player.getInventory().setItem(2,new ItemStack("gold",60,64));
   expect(BufferInventoryActions.withdraw(player,network,id,64)==4,"partial inventory room");
   expect(network.findEntry(id).get().quantity()==92,"partial withdrawal debited 4");
   expect(BufferInventoryActions.withdraw(player,network,999999,64)==0,"stale identity is no-op");
   expect(BufferInventoryActions.deposit(player,network,99,64)==0,"reject invalid source slot");
   expect(BufferInventoryActions.withdraw(player,network,id,999999)==0,"no extra room");
   System.out.println("Buffer GUI actions passed: single/stack deposit, full/partial withdraw, full inventory and stale-click safety.");
 }
}
