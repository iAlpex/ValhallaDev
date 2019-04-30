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

package net.sf.odinms.provider.wz;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;

import net.sf.odinms.tools.data.input.LittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.output.LittleEndianWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Ported Code, see WZFile.java for more info
 */
public class WZTool {
	private static Logger log = LoggerFactory.getLogger(WZTool.class);
        private static byte[] stringKeys;
        private static byte[] q;

/*	public final static char[] MODERN_UNI_KEY = new char[] { (char) 26027, (char) 1353, (char) 52583, (char) 2647,
		(char) 31640, (char) 2695, (char) 26092, (char) 35591, (char) 29845, (char) 27702, (char) 22963, (char) 24105,
		(char) 22946, (char) 32259, (char) 32191, (char) 29899, (char) 21392, (char) 37926, (char) 28440, (char) 34657,
		(char) 54992, (char) 7801, (char) 21164, (char) 21225, (char) 31362, (char) 59422 };
	/**
	 * Actually this is just modernUniKey but expanded to single byte chars
	 */
        /*
	public final static char[] MODERN_KEY = new char[MODERN_UNI_KEY.length * 2];

	static {
		for (int i = 0; i < MODERN_UNI_KEY.length; i++) {
			MODERN_KEY[i * 2 + 1] = (char) (MODERN_UNI_KEY[i] >> 8);
			MODERN_KEY[i * 2]= (char) ((MODERN_UNI_KEY[i]) & 0xFF);
		}
	}
         * */
        static {
            File file = new File("gms.hex");
            try {
            InputStream stream  = new FileInputStream(file);
            q = new byte[(int)file.length()];
            stream.read(q, 0, (int)file.length());
            byte mask = (byte)0xAA;
            stringKeys = new byte[q.length];
            for(int i = 0; i < q.length; i++) {
                stringKeys[i] = (byte)(mask ^ q[i]);
                mask = (byte)((mask + 1) & 0xFF);
            }
            stream.close();
            }
            catch(Exception ex) {

            }
        }

	private WZTool() {

	}

	public static char[] xorCharArray(char[] cypher, char[] key) {
		char[] ret = new char[cypher.length];
		for (int i = 0; i < cypher.length; i++) {
			ret[i] = (char) (cypher[i] ^ key[i]);
		}
		return ret;
	}

	public static String dumpCharArray(char[] arr) {
		String ret = " new char[] {";
		for (char c : arr) {
			ret += "(char) " + ((int) c) + ", ";
		}
		ret = ret.substring(0, ret.length() - 2);
		ret += "};";
		return ret;
	}

	public static void writeEncodedString(LittleEndianWriter leo, String s) throws IOException {
		writeEncodedString(leo, s, true);
	}

	public static void writeEncodedString(LittleEndianWriter leo, String s, boolean unicode) throws IOException {
		if (s.equals("")) {
			leo.write(0);
			return;
		}


		if (unicode) {
			// do unicode
			short umask = (short) 0xAAAA;

			if (s.length() < 0x7F)
				leo.write(s.length());
			else {
				leo.write(0x7F);
				leo.writeInt(s.length());
			}

			for (int i = 0; i < s.length(); i++) {
				char chr = s.charAt(i);

				chr ^= umask;
				umask++;
				leo.writeShort((short)chr);
			}
		} else {
			// non-unicode
			byte mask = (byte) 0xAA;

			if (s.length() <= 127)
				leo.write(-s.length());
			else
				leo.writeInt(s.length());

			char str[] = new char[s.length()];
			for (int i = 0; i < s.length(); i++) {
				byte b2 = (byte) s.charAt(i);
				b2 ^= mask;
				mask++;
				str[i] = (char) b2;
			}
		}
	}

	public static String readDecodedString(LittleEndianAccessor llea) {
            String result = "";
            Integer size = (int)llea.readByte();
            if(size == 0) {
                result = "";
            }
            if(size > 0) {
                if(size == 127) {
                    size = llea.readInt();
                }
                byte[] decrypted = decrypt(llea.read(size * 2));
                result = transStr16KMST(decrypted);
            }
            if (size < 0) {
                if (size == -128) {
                    size = llea.readInt();
                } else {
                    size = -size;
                }
                result = transStr(llea.read(size));
            }
            return result;
	}
        public static String transStr(byte[] input) {
                String ret = "";
                for(int i = 0; i < input.length; i++) {
                    ret += (char)(input[i] ^ stringKeys[i]);
                }
                return ret;
        }
        public static String transStr16KMST(byte[] input) {
            Integer p = 0xAAAA;
            byte pASCII = (byte)0xAA;
            Integer check;
            String s = "";
            for(int i = 0; i < (input.length / 2); i++) {
                if(input[input.length - 1] == 0xAA) {
                    byte part = input[i * 2];
                    check = (part ^ pASCII) & 0xFF;
                }
                else {
                    byte[] temp = new byte[4];
                    int part = getBytes(input, i * 2,2);
                    check = (int)((part ^ p) & 0xFFFF);
                }
                p++;
                pASCII++;
                s += "&#" + check.toString() + ";";
            }
            return s;
        }
        public static int getBytes(byte[] input, int pos, int len) {
            byte[] retbyte = new byte[4];
            int j = 0;
            for (int i = pos; i < (pos + len); i++)
            {
                retbyte[j] = input[i];
                j++;
            }
            int retval = 0;
            for(int i = 0; i < 4; i++) {
                int temp = (4 - 1 - i) * 8;
                retval += (retbyte[i] & 0x000000FF) << temp;
            }
            return retval;
        }
        public static byte[] decrypt(byte[] input) {
            byte[] ret = new byte[input.length];
            for(int i = 0; i < ret.length; i++) {
                ret[i] = (byte)(input[i] ^ q[i]);
            }
            return ret;
        }

	public static String readDecodedStringAtOffset(SeekableLittleEndianAccessor slea, int offset) {
		slea.seek(offset);
		return readDecodedString(slea);
	}

	public static String readDecodedStringAtOffsetAndReset(SeekableLittleEndianAccessor slea, int offset) {
		long pos = 0;
		pos = slea.getPosition();
		slea.seek(offset);
		String ret = readDecodedString(slea);
		slea.seek(pos);
		return ret;
	}

	public static int readValue(LittleEndianAccessor lea) {
		byte b = lea.readByte();
		if (b == -128) {
			return lea.readInt();
		} else {
			return ((int) b);
		}
	}

	public static void writeValue(LittleEndianWriter lew, int val) throws IOException {
		if (val <= 127)
			lew.write(val);
		else {
			lew.write(-128);
			lew.writeInt(val);
		}
	}

	public static float readFloatValue(LittleEndianAccessor lea) {
		byte b = lea.readByte();
		if (b == -128) {
			return lea.readFloat();
		} else {
			return 0;
		}
	}

	public static void writeFloatValue(LittleEndianWriter leo, float val) throws IOException {
		if (val == 0) {
			leo.write(-128);
		} else {
			leo.write(0);
			leo.writeInt(Float.floatToIntBits(val));
		}
	}
}