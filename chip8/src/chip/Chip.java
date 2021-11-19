package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class Chip {

	private char[] memory; // 4kb of 8-bit memory: At position 0x50: The "bios" fontset. At position 0x200: The start of every program.

    private char[] V; // CPU registers: The Chip 8 has 15 8-bit general purpose registers named V0,V1 up to VE. The 16th register is used  for the ‘carry flag’.

    // Index register I and a program counter (pc) which can have a value from 0x000 to 0xFFF. // 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
    private char I;                                                                            // 0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
    private char pc;                                                                           // 0x200-0xFFF - Program ROM and work RAM

    // The stack is used to remember the current location before a jump is performed.
    private char[] stack;

    // Points to next free slot int the stack
    private int stackPointer;

    // This timer is used to delay events in programs/games
    private int delay_timer;

    // This timer to make a beeping sound
    private int sound_timer;

    // This array will be the keyboard state
    private byte[] keys;

    // The 64x32 pixels monochrome (black/white) display
    private byte[] display;

    private boolean needDraw;

    // Resets the chip8 memory and pointers
    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0;
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[64 * 32];

        needDraw = false;
		
		loadFontSet();
    }

      // execute opcode
    public void run() {
        // fetch Opcode
        char opcode = (char)((memory[pc] << 8 | memory[pc + 1]));
		System.out.println(Integer.toHexString(opcode) + " ");

		// decode opcode
        switch (opcode & 0xF000) {

            case 0x0000: {

                switch (opcode & 0x00FF) {

                    case 0x00E0: {//00E0: Clear the screen

                        for(int i=0; i < getDisplay().length; i++)
                            display[i] = 0x0;

                        needDraw = true;
                        pc+=2;
                        ////System.outprintln("CLEARED SCREEN!");
                        break;
                    }

                    case 0x00EE: {//00EE: Retruns from sub
                        stackPointer--;  // 16 levels of stack, decrease stack pointer to prevent overwrite
                        pc = (char) stack[stackPointer]; // Put the stored return address from the stack back into the program counter
                        pc += 2;
                        ////System.outprintln("Returning to " + Integer.toHexString(pc).toUpperCase());
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }

                break;
            }

            case 0x1000: {//1NNN: Jumps to address NNN
                //int NNN = opcode & 0x0FFF;
                pc = (char) (opcode & 0x0FFF);
                ////System.outprintln("Jumping to " + Integer.toHexString(pc).toUpperCase());
                break;
            }

            case 0x2000: {//2NNN: Calls subroutine at NNN
                stack[stackPointer] = pc;
                stackPointer++;
                pc = (char) (opcode & 0x0FFF);
                ////System.outprintln("Calling " + Integer.toHexString(pc) + " from " + Integer.toHexString(stack[stackPointer - 1]).toUpperCase());
                break;
            }

            case 0x3000: {//3XNN: Skips to next instruction if VX equal to NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if(V[x] == nn) {
                    pc += 4;
                    ////System.outprintln("Skipping next instruction (V[" + x +"] == " + nn + ")");
                } else {
                    pc += 2;
                    ////System.outprintln("Not skipping next instruction (V[" + x +"] ("+V[x]+") =/= " + nn + ")");
                }
                break;
            }

            case 0x4000: {//4XNN: Skip next instruction if Vx != NN
                int X = (opcode & 0x0F00) >> 8;
                int NN = opcode & 0x00FF;
                if( V[X] != NN) {
                    pc += 4;
                    ////System.outprintln("Skipping next instruction: V[" + X + "] == " + NN);
                }
                else {
                    pc += 2;
                    ////System.outprintln("Not skipping next instruction (V[" + X + "] != " + NN + ")");
                }
                break;
            }

            case 0x5000: {//5NNN: Skip next instruction if Vx = Vy.
                int X = (opcode & 0x0F00) >> 8; // (take the third spot)
                int Y = (opcode & 0x00F0) >> 4;
                if (V[X] == V[Y]) {
                    pc += 4;
                    ////System.outprintln("Skipping next instruction: V[" + X + "] == V[" + Y + "]");
                }
                else {
                    pc += 2;
                    ////System.outprintln("Not skipping the next instruction: V[" + X + "] != V[" + Y + "]");
                }
                break;
            }

            case 0x6000: { //6XNN: Set VX to NN, v = memory part of the registry, x = index of the registry, NN = value to which we set it.
                int X = (opcode & 0x0F00) >> 8; // (take the third spot)
                V[X] = (char) (opcode & 0x00FF); //(take last two nibble)
                pc += 2;
                ////System.outprintln("Setting V[" + X + "] to " + (int) V[X]);
                break;
            }

            case 0x7000: { //7XNN: Adds NN to VX
                int X = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                //System.outprint("Adding " + nn + " to V[" + X + "] = ");
                V[X] = (char) ((V[X] + nn) & 0xFF); // to avoid overload buffer
                pc += 2;
                //System.outprint((int) V[X] + "\n");
                break;
            }

            case 0x8000: {//Contains more data in last nibble

                switch (opcode & 0x000F) {

                    case 0x0000: {//8XY0: Stores the value of register VY in register VX.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = V[y];
                        //////System.outprintln("Setting V["+x+"] = " + V[y]);
                        pc += 2;
                        break;
                    }

                    case 0x0001: {//8XY1: Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) ((V[x] | V[y]) & 0xFF);
                        //////System.outprintln("Performs a bitwise OR on the values of V[" + x + "] and V[" + y + "] and stores result in Vx");
                        pc += 2;
                        break;
                    }

                    case 0x0002 :{//8XY2: Set Vx = Vx AND Vy.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) (V[x] & V[y]);
                        //////System.outprintln("Performs a bitwise AND on the values of V[" + x + "] and V[" + y + "] and stores result in Vx");
                        pc += 2;
                        break;
                    }

                    case 0x0003 :{//8XY3: Set Vx = Vx XOR Vy.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) ((V[x] ^ V[y]) & 0xFF);
                        //////System.outprintln("Performs a bitwise XOR on the values of V[" + x + "] and V[" + y + "] and stores result in Vx");
                        pc += 2;
                        break;
                    }

                    case 0x0004: {//8XY4: The values of Vx and Vy are added together. If the result is greater than 8 bits (i.e., > 255,)
                                  // VF is set to 1, otherwise 0. Only the lowest 8 bits of the result are kept, and stored in Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        //V[0xF] = V[y] > (0xFF - V[x]) ? (char) 1 : (char) 0;
                        if(V[y] > (0xFF - V[x]))
                            V[0xF] = 1; //carry
                        else
                            V[0xF] = 0;

                        V[x] = (char) ((V[x] + V[y]) & 0xFF);
                        pc += 2;
                        //////System.outprintln("V[" + y + "] > 0xFF - V[" + x + "] ? carry = " + V[0xF]);
                        break;
                    }

                    case 0x0005: {//8XY5: Set Vx = Vx - Vy, set VF = NOT borrow.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        ////System.outprint("V[" + x + "] = " + (int)V[x] + " V[" + y + "] = " + (int)V[y] + ", ");
                        if(V[x] > V[y]) {
                            V[0xF] = 1;
                        //    ////System.outprintln("No Borrow");
                        } else {
                            V[0xF] = 0;
                        //    ////System.outprintln("Borrow");
                        }
                        V[x] = (char)((V[x] - V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }

                    case 0x0006: {//8XY6: Shift VX right by one, VF is set to the least significant bit of VX
                        int x = (opcode & 0x0F00) >> 8;
                        V[0xF] = (char) (V[x] & 0x1);
                        V[x] >>= 1;
                        //////System.outprintln("Shift V[" + x + "] right by one.");
                        pc += 2;
                        break;
                    }

                    case 0x0007: {//8XY7: Set Vx = Vy - Vx, set VF = NOT borrow.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        if(V[x] > V[y])
                            V[0xF] = 0;
                        else
                            V[0xF] = 1;

                        V[x] = (char)((V[y] - V[x]) & 0xFF);
                        //////System.outprintln("V[" + x + "] = V[" + y + "] - V[" + x + "], Applies Borrow if needed");

                        pc += 2;
                        break;
                    }

                    case 0x000E: {//8XYE: Set Vx = Vx SHL 1.
                        int x = (opcode & 0x0F00) >> 8;
                        //V[0xF] = (char) (V[x] >> 7);
                        //V[x] <<= 1;
                        ///*
                        V[0xF] = (char)(V[x] & 0x80);
				        V[x] = (char)(V[x] << 1);
                        //*/
                        //////System.outprintln("Shift V[ " + x + "] << 1 and VF to MSB of VX");
                        pc += 2;
                        break;
                    }

                    default:
                        System.err.println("Unsupported opcode!");
                        System.exit(0);
                        break;

                }
                break;
            }
				
			case 0xA000: {//ANNN: Set I to NNN
			    int NNN = (opcode & 0X0FFF);
                I = (char) NNN;
                pc += 2;
                //////System.outprintln("Setting I to 0x" + Integer.toHexString(I));
                break;
            }

            case 0xB000: {//BNNN Jumps to the address NNN plus V0.
                //pc = (char) ((opcode & 0x0FFF) + V[0]);
                ////System.outprint("Jumps to the address ");
                int NNN = opcode & 0x0FFF;
                int extra = V[0] & 0xFF;
                //System.outprint(NNN + " + " + extra + "\n");
                pc = (char) (NNN + extra);
                break;
            }

            case 0xC000: {//CXNN: Set VX to a random number and NN
                int X = (opcode & 0x0F00) >> 8;
                int NN = opcode & 0x00FF;
                int randomNumber = new Random().nextInt(255) & NN;
                ////System.outprintln("V[" + X + "] has been set to (randomised) " + randomNumber);
                V[X] = (char)randomNumber;
                pc += 2;
                break;
            }
				
			case 0xD000: {//DXYN: Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
                int X = V[(opcode & 0x0F00) >> 8];
                int Y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;//                                                                           HEX    BIN        Sprite
                 /*The interpreter reads n bytes from memory, starting at the address stored in I.        memory[I]     = 0x3C   00111100     ****
                 These bytes are then displayed as sprites on screen at coordinates (Vx, Vy).             memory[I + 1] = 0xC3   11000011   **    **
                 Sprites are XORed onto the existing screen. If this causes any pixels to be erased,      memory[I + 2] = 0xFF   11111111   ********
                 VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it
                 is outside the coordinates of the display, it wraps around to the opposite side of the
                 screen. See instruction 8xy3 for more information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites. */

                V[0xF] = 0; // Reset register VF

                for(int yline = 0; yline < height; yline++) {
                    int pixel = memory[I + yline]; // Fetch the pixel value from the memory starting at location I
                    for(int xline = 0; xline < 8; xline++) {
                        if ((pixel & (0x80 >> xline)) != 0) { // (0x80 >> xline) scan through the byte, one bit at the time
                            int totalX = X + xline;
                            int totalY = Y + yline;

                            totalX = totalX % 64;
                            totalY = totalY % 32;

                            int index = (totalY * 64) + totalX;

                            if(display[index] == 1) // Check if the pixel on the display is set to 1
                                V[0xF] = 1; // to register the collision by setting the VF register

                            display[index] ^= 1; // Set the pixel value by using XOR
                        }
                    }
                }
                pc += 2;
                needDraw = true;
                ////System.outprintln("Drawing at V[" + ((opcode & 0x0F00) >> 8) + "] = " + X + ", V[" + ((opcode & 0x00F0) >> 4) + "] = " + Y);
                break;
			}

            case 0xE000: {

                switch (opcode & 0x0FF) {

                    case 0x009E: {//EX9E: Checks the keyboard, and if the key corresponding to the value of Vx is currently in the down position
                        int x = (opcode & 0x0F00) >> 8;
                        if(keys[V[x]] != 0) {
                            pc += 4;
                        }
                        else {
                            pc += 2;
                            //System.outprint("Not- ");
                        }
                        ////System.outprintln("skipping the next instruction as key[V[" + x + "]] = " + keys[V[x]]);
                        break;
                    }

                    case 0x00A1: {//EXA1: Skip next instruction if key with the value of Vx is not pressed.
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if(keys[key] != 0) {
                            pc += 2;
                            //System.outprint("Not ");
                        }
                        else {
                            pc += 4;
                        }
                        ////System.outprintln("skipping the next instruction as key[V[" + x + "]] = " + keys[V[x]]);
                        break;
                    }

                }
                break;
            }


            case 0xF000: {

                switch (opcode & 0x00FF) {

                    case 0x000A: {//FX00A: Wait for a key press, store the value of the key in Vx.
                        int x = (opcode & 0x0F00) >> 8;

                        for(int i=0; i<keys.length; i++){
                            if(keys[i] == 1) {
                                V[x] = (char) i;
                                pc += 2;
                                break;
                            }
                        }
                        ////System.outprintln("Waited for a key press and then store the value of the key in V[" + x + "]");
                        break;
                    }

                    case 0x0007: {//FX07: Set Vx = delay timer value.
                        int x = (opcode & 0x0F00) >> 8;
                        V[x] = (char) delay_timer;
                        pc += 2;
                        ////System.outprintln("Set V[" + x + "] = " + delay_timer);
                        break;
                    }

                    case 0x0015: {//FX15: Set delay timer = Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        delay_timer = V[x];
                        pc += 2;
                        ////System.outprintln("Set delay_timer to V[" + x + "] = " + (int)V[x]);
                        break;
                    }

                    case 0x0018: {//FX18: Set sound timer = Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        sound_timer = V[x];
                        pc += 2;
                        ////System.outprintln("Set sound timer = " + V[x]);
                        break;
                    }

                    case 0x001E: {//FX1E: Set I = I + Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        ////System.outprintln("Set I = " + Integer.toHexString(I).toUpperCase() + " V[ " + x + "]");
                        I = (char) (I + V[x]);
                        pc += 2;
                        break;
                    }

                    case 0x0029: {//FX29: Sets I to the location of the sprite for the character VX (fontset)
                        int x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (char)(0x050 + (character * 5));
                        ////System.outprintln("Setting I to Character V[" + x + "] = " + (int)V[x] + " Offset to 0x" + Integer.toHexString(I).toUpperCase());
                        pc += 2;
                        break;
                    }

                    case 0x0033: {//FX33 Store a binary-coded decimal value VX in I, I+1 and I+2
                        int x = (opcode & 0x0F00) >> 8;
                        int value = V[x];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10))/ 10;
                        value -= tens * 10;
                        memory[I] = (char)hundreds;
                        memory[I + 1] = (char)tens;
                        memory[I + 2] = (char)value;
                        ////System.outprintln("Storing Binary-Coded Decimal V[" + x + "] = " + (int)(V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds+ ", " + tens + ", " + value + "}");
                        pc += 2;
                        break;
                    }

                    case 0x0055: {//FX55: Stores V0 to VX in memory starting at address I
                        int x = (opcode & 0x0F00) >> 8;

                        for(int i=0; i<=x; i++){
                            memory[I + i] = V[i];
                        }

                        ////System.outprintln("Setting Memory[" + Integer.toHexString(I & 0xFFFF).toUpperCase() + " + n] = V[0] to V[x]");
                        pc += 2;
                        break;
                    }

                    case 0x0065:  {//Fx65 Read registers V0 through Vx from memory starting at location I.
                        int x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i <= x; i++) {
                            V[i] = memory[I + i];
                        }
                        ////System.outprintln("Setting V[0] to V[" + x + "] to the values of merory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");

                        // On the original interpreter, when the operation is done, I = I + X + 1.
                        I = (char) (I + x + 1);

                        pc += 2;
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;

                }
                break;

            }


			default:
				System.err.println("Unsupported opcode!");
				System.exit(0);
				break;	
        }

        if(sound_timer > 0) {
            sound_timer--;
            //Audio.playSound("./beep.wav");
        }
        if(delay_timer > 0)
            delay_timer--;
    }

    public byte[] getDisplay () {
        return display;
    }

    public void loadProgram(String c8File) {
		DataInputStream inputStream = null;
		
        try {
            inputStream = new DataInputStream(new FileInputStream(new File(c8File)));

            int offset = 0;
            while (inputStream.available() > 0) {
                // Program counter starts at 0x200               // ffffffc -> fc
                memory[0x200 + offset] = (char) (inputStream.readByte() & 0xFF); 
                offset++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
				if (inputStream != null) {
						try {
							inputStream.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
		}

    }
	
	public void loadFontSet() {
		for(int i=0; i < ChipData.fontset.length; i++) {
			memory[0x50 + i] = (char) ChipData.fontset[i];
		}
	}

    public boolean needsRedraw() {
        return needDraw;
    }

    public void removeDrawFlag() {
        needDraw = false;
    }
}
