/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.sf.odinms.scripting.npc;

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.scripting.AbstractScriptManager;

/**
 *
 * @author Matze
 */
public class NPCScriptManager extends AbstractScriptManager {

	private Map<MapleClient,NPCConversationManager> cms = new HashMap<MapleClient,NPCConversationManager>();
	private Map<MapleClient,NPCScript> scripts = new HashMap<MapleClient,NPCScript>();
	private static NPCScriptManager instance = new NPCScriptManager();
	
	public synchronized static NPCScriptManager getInstance() {
		return instance;
	}
	
	public void start(MapleClient c, int npc, String filename, MapleCharacter chr) {
		try {
			NPCConversationManager cm = new NPCConversationManager(c, npc, chr);
			if (cms.containsKey(c)) {
				return;
			}
			cms.put(c, cm);
			Invocable iv = getInvocable("npc/" + npc + ".js", c);
            NPCScriptManager npcsm = NPCScriptManager.getInstance();
			if (filename != null) {
				iv = getInvocable("npc/" + filename + ".js", c);
			}
			if (iv == null || npcsm == null || NPCScriptManager.getInstance() == null) { 
				cm.dispose();
				return;
			}
			engine.put("cm", cm);
			NPCScript ns = iv.getInterface(NPCScript.class);
			scripts.put(c, ns);
			if (chr != null) {
				ns.start(chr);
			} else {
				ns.start();
			}
		} catch (Exception e) {
			log.error("Error executing NPC script.", e);
			dispose(c);
			cms.remove(c);
		}
	}
	
	public void action(MapleClient c, byte mode, byte type, int selection) {
		NPCScript ns = scripts.get(c);
		if (ns != null) {
			try {
				ns.action(mode, type, selection);
			} catch (Exception e) {
				log.error("Error executing NPC script.", e);
				dispose(c);
			}
		}
	}
	
	public void dispose(NPCConversationManager cm) {
		cms.remove(cm.getC());
		scripts.remove(cm.getC());
		resetContext("npc/" + cm.getNpc() + ".js", cm.getC());
	}
	
	public void dispose(MapleClient c) {
		NPCConversationManager npccm = cms.get(c);
		if (npccm != null) {
			dispose (npccm);
		}
	}
	
	public NPCConversationManager getCM(MapleClient c) {
		return cms.get(c);
	}
}
