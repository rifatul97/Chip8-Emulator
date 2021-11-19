package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChipFrame extends JFrame implements KeyListener {

    private ChipPanel chipPanel;
    private int[] keyIdToKey;
    private int[] keyBuffer;

    public ChipFrame (Chip c) {
        //setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().right, 320 + getInsets().top + getInsets().bottom));
        chipPanel = new ChipPanel(c);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(chipPanel, BorderLayout.CENTER);

        keyIdToKey = new int[256];
        keyBuffer = new int[16];

        fillKeyIds();
        pack();
        setVisible(true);
        
    }

    private void fillKeyIds () {
        for(int i=0; i<keyIdToKey.length; i++) {
            keyIdToKey[i] = -1;
        }

        keyIdToKey['1'] = 1;
        keyIdToKey['2'] = 2;
        keyIdToKey['3'] = 3;
        keyIdToKey['Q'] = 4;
        keyIdToKey['W'] = 5;
        keyIdToKey['E'] = 6;
        keyIdToKey['A'] = 7;
        keyIdToKey['S'] = 8;
        keyIdToKey['D'] = 9;
        keyIdToKey['Z'] = 0xA;
        keyIdToKey['X'] = 0;
        keyIdToKey['C'] = 0xB;
        keyIdToKey['4'] = 0xC;
        keyIdToKey['R'] = 0xD;
        keyIdToKey['F'] = 0xE;
        keyIdToKey['V'] = 0xF;

    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        System.out.println(keyEvent.getKeyCode());
        if(keyIdToKey[keyEvent.getKeyCode()] != -1) {
            keyBuffer[keyEvent.getKeyCode()] = 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        if(keyIdToKey[keyEvent.getKeyCode()] != -1) {
            keyBuffer[keyIdToKey[keyEvent.getKeyCode()]] = 0;
        }
    }

    public int[] getKeyBuffer() {
        return keyBuffer;
    }
}
