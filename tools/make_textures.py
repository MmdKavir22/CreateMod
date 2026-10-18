import os, struct, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "testmod", "textures")
CLEAR = (0, 0, 0, 0)
def rgba(code, alpha=255):
    return (int(code[1:3],16), int(code[3:5],16), int(code[5:7],16), alpha)
WOOD = {".":CLEAR,"d":rgba("#3d2512"),"m":rgba("#7b4c22"),"l":rgba("#a8733a"),"G":rgba("#1e6b2b"),"g":rgba("#3d9a40"),"h":rgba("#7fd35e")}
WAND = ["................","...........hg...","..........hgggG.",".........hgGgghg","........ggghgGg.","........gGgmgg..",".......hgghdg...",".......gglld....","......hglmd.....","....gGlmd.......","...hglmd........","....lmd.........","...lmd..........","..lmd...........",".lmd............","................"]
BOLT = ["................","................","................","...........hg...","..........hggG..",".........ggGgg..","........glmgg...",".......lmd.G....","......lmd.......",".....lmd........","....lmd.........","...lmd..........","................","................","................","................"]
SHAPE = ["....#....","...###...","...###...","..#####..",".#######.",".#######.",".#######.","..#####..","...###..."]
OUTLINE=rgba("#0b1233"); FILL=rgba("#2f6fe0"); LIGHT=rgba("#4f93ff"); SHINE=rgba("#c4e3ff"); EMPTY=rgba("#1a2440",170)
def pixels(art,palette): return [[palette[c] for c in row] for row in art]
def mana(mode):
    out=[]
    for y,row in enumerate(SHAPE):
        line=[]
        for x,ch in enumerate(row):
            if ch!="#": line.append(CLEAR); continue
            edge=any(not(0<=nx<9 and 0<=ny<len(SHAPE) and SHAPE[ny][nx]=="#") for nx,ny in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
            filled=mode=="full" or (mode=="half" and x<=4)
            if edge: color=OUTLINE
            elif not filled: color=EMPTY
            elif (x,y) in ((3,4),(3,5),(4,3)): color=SHINE
            elif y<5: color=LIGHT
            else: color=FILL
            line.append(color)
        out.append(line)
    return out
def write_png(path,data):
    h,w=len(data),len(data[0]); raw=bytearray()
    for row in data:
        raw.append(0)
        for px in row: raw.extend(px)
    def chunk(tag,body):
        b=tag+body
        return struct.pack(">I",len(body))+b+struct.pack(">I",zlib.crc32(b)&0xffffffff)
    png=b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(bytes(raw),9))+chunk(b"IEND",b"")
    os.makedirs(os.path.dirname(path),exist_ok=True)
    with open(path,"wb") as f: f.write(png)
def main():
    write_png(os.path.join(TEX,"item","test_wand.png"),pixels(WAND,WOOD))
    write_png(os.path.join(TEX,"item","wand_bolt.png"),pixels(BOLT,WOOD))
    for mode in ("full","half","empty"): write_png(os.path.join(TEX,"gui","mana_"+mode+".png"),mana(mode))
if __name__=="__main__": main()
