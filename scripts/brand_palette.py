"""Generates the brand colour scheme in app/.../ui/theme/Theme.kt from the brand red #E1251B.

HCT (CAM16 hue and chroma + L* tone), as in Material Color Utilities, with no dependencies: the solver finds the
colour of a given hue and tone with the highest chroma up to the requested one. Results match Material Color
Utilities within one step per channel. Run: python3 scripts/brand_palette.py
"""
import math
def lin(c):
    c/=255; return (c/12.92 if c<=0.04045 else ((c+0.055)/1.055)**2.4)*100
def delin(c):
    c/=100; v = 12.92*c if c<=0.0031308 else 1.055*c**(1/2.4)-0.055
    return v*255
def y_from_lstar(l):
    ft=(l+16)/116; return (ft**3 if ft**3>216/24389 else l/(24389/27))*100
def lstar_from_y(y):
    y/=100; f = y**(1/3) if y>216/24389 else (24389/27*y+16)/116
    return 116*f-16
W=[95.047,100.0,108.883]
M=[[0.401288,0.650173,-0.051461],[-0.250268,1.204414,0.045854],[-0.002079,0.048952,0.953127]]
mul=lambda m,v:[sum(m[i][j]*v[j] for j in range(3)) for i in range(3)]
La=(200/math.pi)*y_from_lstar(50)/100
f=1.0; c=0.69; nc=f
d=max(0,min(1,f*(1-(1/3.6)*math.exp((-La-42)/92))))
rgbW=mul(M,W); rgbD=[d*(100/x)+1-d for x in rgbW]
k=1/(5*La+1); k4=k**4; k4F=1-k4
fl=k4*La+0.1*k4F*k4F*(5*La)**(1/3)
n=y_from_lstar(50)/W[1]; z=1.48+math.sqrt(n); nbb=0.725/n**0.2; ncb=nbb
af=[(fl*rgbD[i]*rgbW[i]/100)**0.42 for i in range(3)]
rA=[400*x/(x+27.13) for x in af]
aw=(2*rA[0]+rA[1]+0.05*rA[2])*nbb
XYZ2RGB=[[3.2413774792388685,-1.5376652402851851,-0.49885366846268053],[-0.9691452513005321,1.8758853451067872,0.04156585323272394],[0.05562093689691305,-0.20395524564742123,1.0571799111220335]]
RGB2XYZ=[[0.41233895,0.35762064,0.18051042],[0.2126,0.7152,0.0722],[0.01932141,0.11916382,0.95034478]]
def cam(hexs):
    r,g,b=[lin(int(hexs[i:i+2],16)) for i in (0,2,4)]
    xyz=mul(RGB2XYZ,[r,g,b])
    cc=mul(M,xyz); out=[]
    for i in range(3):
        rd=rgbD[i]*cc[i]; x=(fl*abs(rd)/100)**0.42
        out.append(math.copysign(1,rd)*400*x/(x+27.13))
    ra,ga,ba=out
    a=(11*ra-12*ga+ba)/11; bb=(ra+ga-2*ba)/9
    u=(20*ra+20*ga+21*ba)/20; p2=(40*ra+20*ga+ba)/20
    h=math.degrees(math.atan2(bb,a))%360
    J=100*((p2*nbb)/aw)**(c*z)
    hp=h+360 if h<20.14 else h
    e=0.25*(math.cos(math.radians(hp)+2)+3.8)
    t=(50000/13*e*nc*ncb)*math.hypot(a,bb)/(u+0.305)
    C=t**0.9*(1.64-0.29**n)**0.73*math.sqrt(J/100)
    return h,C,lstar_from_y(xyz[1])
def inv(J,C,h):
    alpha=0 if C==0 or J==0 else C/math.sqrt(J/100)
    t=(alpha/(1.64-0.29**n)**0.73)**(1/0.9)
    hr=math.radians(h); e=0.25*(math.cos(hr+2)+3.8)
    ac=aw*(J/100)**(1/c/z); p1=e*(50000/13)*nc*ncb; p2=ac/nbb
    hs,hc=math.sin(hr),math.cos(hr)
    gam=23*(p2+0.305)*t/(23*p1+11*t*hc+108*t*hs)
    a=gam*hc; b=gam*hs
    A=[(460*p2+451*a+288*b)/1403,(460*p2-891*a-261*b)/1403,(460*p2-220*a-6300*b)/1403]
    F=[]
    for i,x in enumerate(A):
        base=max(0,27.13*abs(x)/(400-abs(x)))
        F.append(math.copysign(1,x)*(100/fl)*base**(1/0.42)/rgbD[i])
    xyz=[1.86206786*F[0]-1.01125463*F[1]+0.14918677*F[2],0.38752654*F[0]+0.62144744*F[1]-0.00897398*F[2],-0.01584150*F[0]-0.03412294*F[1]+1.04996444*F[2]]
    return xyz
def solve_fixed(h,C,T):
    Y=y_from_lstar(T); lo,hi=0.0,100.0
    for _ in range(60):
        J=(lo+hi)/2; xyz=inv(J,C,h)
        if xyz[1]<Y: lo=J
        else: hi=J
    rgb=mul(XYZ2RGB,inv((lo+hi)/2,C,h))
    ok=all(-1e-4<=v<=100.0001 for v in rgb)
    return ok,rgb
def hct(h,C,T):
    if T<=0.0001: return "000000"
    if T>=99.999: return "FFFFFF"
    ok,rgb=solve_fixed(h,C,T)
    if not ok:
        lo,hi=0.0,C
        for _ in range(40):
            m=(lo+hi)/2
            if solve_fixed(h,m,T)[0]: lo=m
            else: hi=m
        ok,rgb=solve_fixed(h,lo,T)
    return "".join("%02X"%round(max(0,min(255,delin(max(0,min(100,v)))))) for v in rgb)


h, source_chroma, _ = cam("E1251B")
P=lambda t:hct(h,source_chroma,t); S=lambda t:hct(h,24,t); T=lambda t:hct(h,32,t)
N=lambda t:hct(h,2,t); V=lambda t:hct(h,4,t)
light=dict(primaryContainer=P(90),onPrimaryContainer=P(10),inversePrimary=P(80),
 secondary=S(40),onSecondary=S(100),secondaryContainer=S(90),onSecondaryContainer=S(10),
 tertiary=T(40),onTertiary=T(100),tertiaryContainer=T(90),onTertiaryContainer=T(10),
 onBackground=N(10),onSurface=N(10),surfaceVariant=V(90),onSurfaceVariant=V(30),
 inverseSurface=N(20),inverseOnSurface=N(95),outline=V(50),outlineVariant=V(80),
 surfaceDim=N(87),surfaceBright=N(98),surfaceContainerLowest=N(100),surfaceContainerLow=N(96),
 surfaceContainer=N(94),surfaceContainerHigh=N(92),surfaceContainerHighest=N(90))
dark=dict(primaryContainer=P(30),onPrimaryContainer=P(90),inversePrimary=P(40),
 secondary=S(80),onSecondary=S(20),secondaryContainer=S(30),onSecondaryContainer=S(90),
 tertiary=T(80),onTertiary=T(20),tertiaryContainer=T(30),onTertiaryContainer=T(90),
 onBackground=N(90),onSurface=N(90),surfaceVariant=V(30),onSurfaceVariant=V(80),
 inverseSurface=N(90),inverseOnSurface=N(20),outline=V(60),outlineVariant=V(30),
 surfaceDim=N(6),surfaceBright=N(24),surfaceContainerLowest=N(4),surfaceContainerLow=N(10),
 surfaceContainer=N(12),surfaceContainerHigh=N(17),surfaceContainerHighest=N(22))
for name,d in (("light",light),("dark",dark)):
  print(name)
  for k,v in d.items(): print(f"    {k} = Color(0xFF{v}),")
