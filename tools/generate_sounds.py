import numpy as np, subprocess, os
from scipy.io import wavfile
from scipy.signal import butter, lfilter
SR=44100
out="/home/claude/dedsafio-space/src/main/resources/assets/dedsafio/sounds/supernova"
os.makedirs(out,exist_ok=True)
rng=np.random.default_rng(7)
def t(d): return np.linspace(0,d,int(SR*d),endpoint=False)
def lp(x,fc,o=4): b,a=butter(o,fc/(SR/2),'low'); return lfilter(b,a,x)
def hp(x,fc,o=4): b,a=butter(o,fc/(SR/2),'high'); return lfilter(b,a,x)
def bp(x,lo,hi): b,a=butter(2,[lo/(SR/2),hi/(SR/2)],'band'); return lfilter(b,a,x)
def noise(d): return rng.standard_normal(int(SR*d))
def env(x,a,r):
    n=len(x); e=np.ones(n); na=int(a*SR); nr=int(r*SR)
    if na>0: e[:na]=np.linspace(0,1,na)
    if nr>0: e[-nr:]=np.minimum(e[-nr:],np.linspace(1,0,nr))
    return x*e
def norm(x,peak=0.9):
    m=np.max(np.abs(x)); return x/m*peak if m>0 else x
def stereo(x,pan=None):
    if pan is None: return np.stack([x,x],1)
    return np.stack([x*np.sqrt(1-pan),x*np.sqrt(pan)],1)
def save(name,x):
    x=np.clip(x,-1,1)
    wav="/tmp/%s.wav"%name
    wavfile.write(wav,SR,(x*32767).astype(np.int16))
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",wav,"-c:a","libvorbis","-q:a","4",f"{out}/{name}.ogg"],check=True)

# star_appear: brillo ascendente con armonicos
d=4; tt=t(d); f=np.linspace(180,520,len(tt)); ph=2*np.pi*np.cumsum(f)/SR
x=sum(np.sin(ph*k)*(0.6/k) for k in (1,2,3,5))*(1+0.25*np.sin(2*np.pi*5*tt))
x+=0.15*hp(noise(d),4000)*np.linspace(0,1,len(tt))
save("star_appear",stereo(norm(env(x,0.8,1.2),0.7)))

# energy_charge: barrido ascendente 12 s con trémolo creciente
d=12; tt=t(d); f=60+900*(tt/d)**2.2; ph=2*np.pi*np.cumsum(f)/SR
trem=1-0.5*(tt/d)*(0.5+0.5*np.sin(2*np.pi*(2+10*tt/d)*tt))
x=(np.sin(ph)+0.5*np.sin(2*ph)+0.25*np.sign(np.sin(ph*0.5)))*trem
x+=0.25*lp(noise(d),1800)*(tt/d)
x*= (0.15+0.85*(tt/d)**1.5)
save("energy_charge",stereo(norm(env(x,0.5,0.3),0.8)))

def boom(d,sub,scale):
    tt=t(d)
    f=sub*np.exp(-tt*0.9)+22; ph=2*np.pi*np.cumsum(f)/SR
    body=np.sin(ph)*np.exp(-tt*0.55)
    rumble=lp(noise(d),240)*np.exp(-tt*0.7)*3
    crack=hp(noise(d),600)*np.exp(-tt*9)*1.2
    return norm(body*1.2+rumble+crack*scale,0.95)
x=boom(6,90,0.8); x=env(x,0.004,0.5)
save("explosion_1",stereo(x))

# shockwave: ruido filtrado con barrido de frecuencia
d=5; tt=t(d); n=noise(d); y=np.zeros_like(n); seg=2048
for i in range(0,len(n),seg):
    fc=1800*np.exp(-3*i/len(n))+120
    y[i:i+seg]=lp(n[i:i+seg+0],min(fc,18000),2)[:len(y[i:i+seg])]
y*=np.sin(np.pi*np.minimum(1,tt/d*1.2))**2*np.exp(-tt*0.4)
save("shockwave",stereo(norm(env(y,0.3,1.0),0.85)))

# aftermath: dron oscuro con modulación lenta
d=14; tt=t(d)
x=np.sin(2*np.pi*43*tt)*0.5+np.sin(2*np.pi*64.5*tt+np.sin(2*np.pi*0.2*tt)*2)*0.3
x+=lp(noise(d),380)*0.8*(0.6+0.4*np.sin(2*np.pi*0.13*tt))
x+=0.06*bp(noise(d),1500,3500)*(0.5+0.5*np.sin(2*np.pi*0.31*tt))
save("aftermath",stereo(norm(env(x,2.5,3.5),0.6)))

x=boom(8,70,1.3); x+=0.5*boom(8,45,0.2)*np.concatenate([np.zeros(int(0.25*SR)),np.ones(len(x)-int(0.25*SR))])
x=env(x,0.003,0.8)
save("explosion_2",stereo(norm(x,0.97)))

# flash: zumbido agudo que decae (sensación de oído amortiguado)
d=5; tt=t(d)
x=(np.sin(2*np.pi*3150*tt)*0.5+np.sin(2*np.pi*4720*tt)*0.2)*np.exp(-tt*0.9)
x+=lp(noise(d),500)*np.exp(-tt*2.5)*0.8
save("flash",stereo(norm(env(x,0.01,0.8),0.6)))

# energy_beam: barrido con paneo de izquierda a derecha
d=3.5; tt=t(d); f=300+2600*np.minimum(1,tt/2.2)**1.5; ph=2*np.pi*np.cumsum(f)/SR
x=(np.sin(ph)+0.4*np.sin(2*ph))*np.sin(np.pi*tt/d)**1.2
x+=0.4*bp(noise(d),1200,7000)*np.sin(np.pi*tt/d)
x=norm(x,0.8); pan=np.clip(tt/d,0,1)
save("energy_beam",np.stack([x*np.cos(pan*np.pi/2),x*np.sin(pan*np.pi/2)],1))
print("ok")
