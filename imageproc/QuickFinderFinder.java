import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

public class QuickFinderFinder extends Component {
    BufferedImage img;


    // line 7, 1 pixel per module would be a special case I don't handle
    int lasty = 13;

    long ave = 0;
    int modwid;
    int finds;

    int[] fx = new int[256];
    int[] fy = new int[256];
    int[] fw = new int[256];
    int[] fh = new int[256];

    int width, height;

    int getlum(int y, int x) {
        int argb = img.getRGB(x,y);
        argb = argb >> 8;
        argb = argb & 255;
        return argb;
    }

    int iabs(int a)
    {
        if (a < 0)
            return -a;
        return a;
    }

    int center(int x, int y, long lave)
    {
        // use u d l r
        int r, v, s;
        int fl, fr, ft, fb;
        int fl1, fr1, ft1, fb1;

        for (r = x - 1; r > 0; r--)
            if (getlum(y, r) > lave)
                break;
        s = r;
        for (; s > 0; s--)
            if (getlum(y, s) < lave)
                break;
        fl = r - s;
        for (; s > 0; s--)
            if (getlum(y, s) > lave)
                break;
        fl1 = r - s - fl;

        r++;
        for (v = x + 1; v < width; v++)
            if (getlum(y, v) > lave)
                break;
        s = v;
        for (; s < width; s++)
            if (getlum(y, s) < lave)
                break;
        fr = s - v;
        for (; s < width; s++)
            if (getlum(y, s) > lave)
                break;
        fr1 = s - v - fr;
        v--;
        x = (r + v) / 2;
        fx[finds] = x;
        fw[finds] = v - r;

        for (r = y - 1; r > 0; r--)
            if (getlum(r, x) > lave)
                break;
        s = r;
        for (; s > 0; s--)
            if (getlum(s, x) < lave)
                break;
        ft = r - s;
        for (; s > 0; s--)
            if (getlum(s, x) > lave)
                break;
        ft1 = r - s - ft;

        r++;
        for (v = y + 1; v < height; v++)
            if (getlum(v, x) > lave)
                break;
        s = v;
        for (; s < height; s++)
            if (getlum(s, x) < lave)
                break;
        fb = s - v;
        for (; s < height; s++)
            if (getlum(s, x) > lave)
                break;
        fb1 = s - v - fb;
        v--;
        y = (r + v) / 2;
        fy[finds] = y;
        fh[finds] = v - r;

        if (fw[finds] * 3 < fh[finds] * 2)
            return 0;
        if (fw[finds] * 2 > fh[finds] * 3)
            return 0;
        if (0 != modwid) {
            if (fw[finds] * 3 < modwid || fw[finds] > modwid)
                return 0;
            if (fh[finds] * 3 < modwid || fh[finds] > modwid)
                return 0;
            // for j==0  too could check lrtb for same width and trace out a square - already checked in scan direction but not perpindicular
            if (fl * 2 > modwid || fr * 2 > modwid || ft * 2 > modwid || fb * 2 > modwid)
                return 0;
            if (fl1 * 2 > modwid || fr1 * 2 > modwid || ft1 * 2 > modwid || fb1 * 2 > modwid)
                return 0;
        }
        v = finds++;
        return fw[v] + fh[v];
    }

    int runs[] = new int[8];

    int checkfinder()
    {
        int a, b, c, d, e, m;
        a = runs[1];
        e = runs[5];
        if (iabs(a - e) > (a + e + 1) / 4)
            return 0;
        b = runs[2];
        d = runs[4];
        if (iabs(b - d) > (b + d + 1) / 4)
            return 0;
        if ((a + e) * 2 < (b + d))
            return 0;
        if ((b + d) * 2 < (a + e))
            return 0;
        c = runs[3];
        if (0 != modwid) {
            if (c * 14 < modwid * 5)
                return 0;
            if (c * 10 > modwid * 7)
                return 0;
        }
        m = a + e + (b + d + 1) / 2;
        if (iabs(c - m) > (c + m) / 4)
            return 0;
        return 1;
    }

    byte findit()
    {
        int x0, x, y, r, xx, mx;
        byte i, b, v;

        // 13 for 2 pixel min, do line 7 if simple 1 ppmod
        finds = 0;
        for (y = lasty; y < height || y < width; y += 2, lasty += 2) {
            x0 = 0;
            if (y >= height)     // off bottom, don't count
                x0 = 1 + y - height;
            mx = y;
            if( y > width )
                mx = width;
            //            System.out.println(y+" "+ave+" "+mx+" "+x0);
            ave = 0;
            for (x = x0; x < mx ; x++)
                ave += getlum(y - x, x);
            ave = ave / (mx - x0);
            b = 0; r = 0; i = 0;
            // Note that we only need the current 5 runs, not a  list
            runs[i] = 0;
            for (x = x0; x < mx; x++) {
                if( getlum(y - x, x) < ave )
                    v = 1;
                else
                    v = 0;
                if (v == b) {
                    r++;
                    continue;
                }
                b = v;

                runs[i++] = r;
                runs[i] = 0;
                r = 1;
                if (i > 6) {
                    for (v = 0; v < 6; v++)
                        runs[v] = runs[v + 2];
                    i -= 2;
                }
                if (i < 6)
                    continue;
                //System.out.println(runs[1]+" "+runs[2]+" "+runs[3]+" "+runs[4]+" "+runs[5]);
                if (0 == checkfinder())
                    continue;
                xx = x - runs[5] - runs[4] - (runs[3] / 2);
                modwid = center(xx, y - xx, ave);
                if (0 != modwid)
                    return 1;
            }
        }
        return 0;
    }

    void findnexty(int x, int y)
    {
        byte b = 0;
        byte v;
        byte i = 0;
        int r = 0;
        long avey = ave * 64;
        runs[0] = 0;
        for (; y < height; y++) {
            avey += getlum(y, x);
            avey -= avey / 64;
            if( getlum(y, x) <= avey / 64 )
                v = 1;
            else
                v = 0;

            if (v == b) {
                r++;
                if (y + 1 != height)
                    continue;
            }
            b = v;
            runs[i++] = r;
            runs[i] = 0;
            r = 1;
            if (i > 6) {
                for (v = 0; v < 6; v++)
                    runs[v] = runs[v + 2];
                i -= 2;
            }
            if (i < 6)
                continue;
            if (runs[1] * 8 < modwid || runs[1] * 4 > modwid)
                continue;
            if (0 == checkfinder())
                continue;
            center(x, y - runs[5] - runs[4] - runs[3] / 2, avey / 64);
        }
        return;
    }

    void findnextx(int x, int y)
    {
        byte b = 0;
        byte v;
        byte i = 0;
        int r = 0;
        long avex = ave * 64;
        runs[0] = 0;
        for (; x < width; x++) {
            avex += getlum(y, x);
            avex -= avex / 64;
            if( getlum(y, x) <= avex / 64 )
                v = 1;
            else
                v = 0;

            if (v == b) {
                r++;
                if (x + 1 != width)
                    continue;
            }
            b = v;
            runs[i++] = r;
            runs[i] = 0;
            r = 1;
            if (i > 6) {
                for (v = 0; v < 6; v++)
                    runs[v] = runs[v + 2];
                i -= 2;
            }
            if (i < 6)
                continue;
            if (runs[1] * 8 < modwid || runs[1] * 4 > modwid)
                continue;
            if (0 == checkfinder())
                continue;
            center(x - runs[5] - runs[4] - (runs[3] / 2), y, avex / 64);
        }
        return;
    }

    int TEST(int x,int y) { return (x*y/modwid); }

    void findfinders() {


        lasty = 13;
        //    for (;;) {
        width = img.getWidth();
        height = img.getHeight();

        while( true ) {

        if (0 == findit())
            return;
        findnexty(fx[0], fy[0]);
        findnextx(fx[0], fy[0]);

        //        System.out.println("Found2:" + finds);
        int i,j;


        j = finds;
        i = 1;
        while (i < j) {
            findnexty(fx[i], fy[i]);
            findnextx(fx[i], fy[i]);
            i++;
        }

        i = j + 1;
        j = finds;
        while (i < j) {
            findnexty(fx[i], fy[i]);
            findnextx(fx[i], fy[i]);
            i++;
        }

        if (finds < 3) {
            // try harder, misalignment
            findnexty(fx[0] - fw[0] / 2, fy[0]);
            findnexty(fx[0] + fw[0] / 2, fy[0]);
            findnextx(fx[0], fy[0] - fh[0] / 2);
            findnextx(fx[0], fy[0] + fh[0] / 2);
        }

        for (i = 0; i < finds - 1; i++)
            for (j = i + 1; j < finds; j++)
                if (iabs(fx[i] - fx[j]) < modwid / 2 && iabs(fy[i] - fy[j]) < modwid / 2) {     // coincident centers
                    //                fprintf(stderr, "DUP - %d,%d %d %d\n", fx[i], fy[i], fw[i], fh[i]);
                    if (j < finds - 1) {
                        fx[j] = fx[finds - 1];
                        fy[j] = fy[finds - 1];
                        fw[j] = fw[finds - 1];
                        fh[j] = fh[finds - 1];
                        j--;
                    }
                    finds--;
                }

        int besti = 1;
        int bestj = 2;
        int bestk = 0;
        if (finds > 2) {
            for (i = 1; i < finds - 1; i++)
                for (j = i + 1; j < finds; j++) {
                    int k, m;
                    // smallest side of largest rectangle

                    k = TEST(iabs(fx[0] - fx[i]), iabs(fy[0] - fy[i]));
                    m = TEST(iabs(fx[0] - fx[j]), iabs(fy[0] - fy[j]));
                    if (m > k)
                        k = m;
                    m = TEST(iabs(fx[j] - fx[i]), iabs(fy[j] - fy[i]));
                    if (m > k)
                        k = m;
                    if (k > bestk) {
                        besti = i;
                        bestj = j;
                        bestk = k;
                    }
                    //                    fprintf(stderr, "A %d %d = %d\n", i, j, k);
                }

        }


        // pick most likely 3
        for (i = 0; i < finds; i++) {
            //            fprintf(stderr, "%d : %d,%d %d %d\n", (i == 0 || i == besti || i == bestj), fx[i], fy[i], fw[i], fh[i]);
            System.out.println(i+":"+fx[i]+":"+fy[i]+":"+fw[i]+":"+fh[i]);
            if(i == 0 || i == besti || i == bestj) {

                // ONE OF THE FOUND

                for (j = 0; j < fw[i]; j++)
                    img.setRGB(  fx[i] - fw[i] / 2 + j , fy[i], 255<<8 );
                for (j = 0; j < fh[i]; j++)
                    img.setRGB(  fx[i], fy[i] - fh[i] / 2 + j , 255<<16 );
            }
        }


        if( finds > 2 )
            break;
    }


    }







    public void paint(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }

    public QuickFinderFinder(String s)  {
        try {
            img = ImageIO.read(new File(s));
            findfinders();
        } catch (IOException e) {
        }
    }
    public Dimension getPreferredSize() {
        if (img == null) 
            return new Dimension(10,10);
        else 
            return new Dimension(img.getWidth(null), img.getHeight(null));
    }
    public static void main(String[] args) {
        JFrame f = new JFrame("Quick Finder Finder");

        f.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        f.add(new QuickFinderFinder(args[0]));
        f.pack();
        f.setVisible(true);
    }
}