from pathlib import Path
import sys,subprocess,json,os
root=Path.cwd();sys.path.insert(0,str(root/'tools'))
import test_foundation
original=subprocess.run
jhome=Path('C:/Program Files/Java/jdk-25.0.4/bin')
lock=json.loads((root/'dependencies.lock.json').read_text())
classes=root/'.test-tmp/unicode-probe-classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join([str(classes),str(root/'target/test-classes'),str(root/'target/classes')]+[str(root/'.audit-cache/m2'/e['jar']) for e in lock['dependencies']])
original([str(jhome/'javac.exe'),'-cp',cp,'-d',str(classes),str(root/'.test-tmp/UnicodePlaceHttpProbe.java')],check=True)
def run(command,*args,**kwargs):
 if Path(str(command[0])).name.lower()=='mvn.cmd':
  return original([str(jhome/'java.exe'),'-cp',cp,'org.dzoka.UnicodePlaceHttpProbe'],*args,**kwargs)
 return original(command,*args,**kwargs)
test_foundation.subprocess.run=run
sys.argv=['test_foundation.py','--pg-bin','C:/Program Files/PostgreSQL/18/bin','--maven','D:/Projects/ai/dzoka/.audit-cache/apache-maven-3.9.16/bin/mvn.cmd','--full']
raise SystemExit(test_foundation.main())
