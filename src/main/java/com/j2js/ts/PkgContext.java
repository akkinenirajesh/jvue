package com.j2js.ts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.j2js.J2JSSettings;
import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtRegistry;

public class PkgContext {

	private Map<String, PkgContext> pkgs = new HashMap<>();
	private Map<String, TypeContext> clss = new HashMap<>();
	private PkgContext parent;
	private String name;

	public PkgContext(PkgContext parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public TypeContext get(TypeDeclaration type) {
		String name = type.getClassName();
		String[] split = name.split("\\.");
		PkgContext pkg = this;
		for (int i = 0; i < split.length - 1; i++) {
			pkg = pkg.getPkg(split[i]);
		}
		return pkg.getType(type, split[split.length - 1]);
	}

	private TypeContext getType(TypeDeclaration type, String name) {
		String[] split = name.split("\\$");
		if (split.length == 1) {
			TypeContext cls = clss.get(split[0]);
			if (cls == null) {
				cls = new TypeContext(type);
				clss.put(split[0], cls);
			}
			return cls;
		} else {
			TypeContext s = clss.get(split[0]);
			for (int i = 1; i < split.length - 1; i++) {
				s = s.getAnonymous(split[i]);
			}
			TypeContext cls = s.getAnonymous(split[split.length - 1], type);
			return cls;
		}
	}

	private PkgContext getPkg(String name) {
		PkgContext p = pkgs.get(name);
		if (p == null) {
			p = new PkgContext(this, name);
			pkgs.put(name, p);
		}
		return p;
	}

	public void write(File base) {
		clss.forEach((s, st) -> {
			File file = new File(base, s + "." + J2JSSettings.ext);
			try {
				PrintStream single = new PrintStream(new FileOutputStream(file));
				ExtRegistry.get().invoke("file.create", single, null);
				st.write(single);
				single.flush();
				single.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		pkgs.forEach((a, b) -> b.write(base));
	}

	public void write(PrintStream ps) {
		clss.forEach((s, st) -> {
			try {
				st.write(ps);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		pkgs.forEach((a, b) -> b.write(ps));
	}

	@Override
	public String toString() {
		if (parent != null) {
			return parent + "." + name;
		}
		return "";
	}
}
