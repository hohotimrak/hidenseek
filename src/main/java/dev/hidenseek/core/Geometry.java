package dev.hidenseek.core;

/** Conservative interval geometry. Closed boxes deliberately retain boundary ambiguity. */
public final class Geometry {
 private Geometry() {}
 public record Point(double x, double y, double z) {}
 public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {
  public Box { if (x0>x1 || y0>y1 || z0>z1) throw new IllegalArgumentException("box"); }
  public boolean contains(Point p) { return p.x>=x0 && p.x<=x1 && p.y>=y0 && p.y<=y1 && p.z>=z0 && p.z<=z1; }
 }
 public enum Truth { TRUE, FALSE, UNKNOWN }
 public record Constraint(String id, String kind, double a, double b, double c, double d, boolean answer, long tick) {
  public boolean test(Point p) {
   return switch(kind) {
    case "east" -> p.x>a;
    case "south" -> p.z>a;
    case "above" -> p.y>a;
    case "circle", "distance" -> square(p.x-a)+square(p.z-b)<=square(c);
    case "rectangle", "sector" -> p.x>=a && p.x<=c && p.z>=b && p.z<=d;
    case "bearing" -> inWedge(p.x-a,p.z-b,c);
    default -> throw new IllegalArgumentException(kind);
   };
  }
  public Truth classify(Box q) {
   return switch(kind) {
    case "east" -> greater(q.x0,q.x1,a);
    case "south" -> greater(q.z0,q.z1,a);
    case "above" -> greater(q.y0,q.y1,a);
    case "circle", "distance" -> {
     double min=square(gap(a,q.x0,q.x1))+square(gap(b,q.z0,q.z1));
     double max=Math.max(square(q.x0-a),square(q.x1-a))+Math.max(square(q.z0-b),square(q.z1-b));
     yield max<=c*c ? Truth.TRUE : min>c*c ? Truth.FALSE : Truth.UNKNOWN;
    }
    case "rectangle", "sector" -> q.x0>=a && q.x1<=c && q.z0>=b && q.z1<=d ? Truth.TRUE :
      q.x1<a || q.x0>c || q.z1<b || q.z0>d ? Truth.FALSE : Truth.UNKNOWN;
    case "bearing" -> {
     double theta=c*Math.PI/4,lo=theta-Math.PI/8,hi=theta+Math.PI/8;
     double min1=Double.POSITIVE_INFINITY,max1=Double.NEGATIVE_INFINITY,min2=min1,max2=max1;
     for(double x:new double[]{q.x0-a,q.x1-a})for(double z:new double[]{q.z0-b,q.z1-b}){double v1=Math.cos(lo)*z-Math.sin(lo)*x,v2=Math.sin(hi)*x-Math.cos(hi)*z;min1=Math.min(min1,v1);max1=Math.max(max1,v1);min2=Math.min(min2,v2);max2=Math.max(max2,v2);}
     yield max1< -1e-8||max2< -1e-8?Truth.FALSE:min1>=1e-8&&min2>=1e-8?Truth.TRUE:Truth.UNKNOWN;
    }
    default -> Truth.UNKNOWN;
   };
  }
  public boolean excludes(Box box) { Truth t=classify(box); return t!=Truth.UNKNOWN && (t==Truth.TRUE)!=answer; }
 }
 private static Truth greater(double lo,double hi,double pivot) { return lo>pivot?Truth.TRUE:hi<=pivot?Truth.FALSE:Truth.UNKNOWN; }
 private static double gap(double v,double lo,double hi) { return v<lo?lo-v:v>hi?v-hi:0; }
 private static double square(double v) { return v*v; }
 private static boolean inWedge(double x,double z,double sector){double theta=sector*Math.PI/4;return Math.cos(theta-Math.PI/8)*z-Math.sin(theta-Math.PI/8)*x>=-1e-8&&Math.sin(theta+Math.PI/8)*x-Math.cos(theta+Math.PI/8)*z>=-1e-8;}
}
