package io.github.r3neer.clingingreoriented.surface;

import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import io.github.r3neer.clingingreoriented.geometry.SweptAabb;
import java.util.Optional;
import net.minecraft.world.phys.AABB;

/** Vanilla block-collision support and bounded swept-floor queries. */
public final class VanillaLandingSurfaceProvider implements LandingSurfaceProvider {
    private static final double PROBE = 1.0E-4D;
    private static final double TIE = 1.0E-9D;

    @Override public Optional<LocalContact> currentSupport(Query query) {
        if (!query.entity().onGround()) return Optional.empty();
        var body = query.entity().getBoundingBox();
        var probe = body.inflate(PROBE);
        for (var shape : query.entity().level().getBlockCollisions(query.entity(), probe)) {
            for (var box : shape.toAabbs()) {
                if (FaceGeometry.touching(body, box, query.gravity()))
                    return Optional.of(contact(box,query));
            }
        }
        return Optional.empty();
    }

    @Override public Optional<LocalSweep> sweep(Query query,AABB startBody,AABB endBody){
        AABB swept=new AABB(Math.min(startBody.minX,endBody.minX),Math.min(startBody.minY,endBody.minY),Math.min(startBody.minZ,endBody.minZ),
            Math.max(startBody.maxX,endBody.maxX),Math.max(startBody.maxY,endBody.maxY),Math.max(startBody.maxZ,endBody.maxZ)).inflate(PROBE);
        double earliestAny=Double.POSITIVE_INFINITY;
        SweptAabb.Hit bestLanding=null;AABB bestBox=null;
        var desired=query.gravity().getOpposite();
        for(var shape:query.entity().level().getBlockCollisions(query.entity(),swept))for(var box:shape.toAabbs()){
            var any=SweptAabb.hit(startBody,endBody,box);
            if(any.isPresent())earliestAny=Math.min(earliestAny,any.get().fraction());
            var landing=SweptAabb.hitForNormal(startBody,endBody,box,desired);
            if(landing.isPresent()&&(bestLanding==null||landing.get().fraction()<bestLanding.fraction()-TIE)){
                bestLanding=landing.get();bestBox=box;
            }
        }
        if(bestLanding==null||bestLanding.fraction()>earliestAny+TIE)return Optional.empty();
        return Optional.of(new LocalSweep(contact(bestBox,query),bestLanding.fraction()));
    }

    @Override public boolean revalidate(Query query, LocalContact contact) {
        if (contact == null) return false;
        AABB stored=parse(contact.localId());
        if(stored==null)return false;
        for (var shape : query.entity().level().getBlockCollisions(query.entity(), stored.inflate(PROBE)))
            for (var box : shape.toAabbs()) if(identity(box).equals(contact.localId()))return true;
        return false;
    }

    private static LocalContact contact(AABB box,Query query){return new LocalContact(identity(box),0L,FaceGeometry.vector(query.gravity().getOpposite()));}
    private static String identity(AABB box) {
        return Double.toHexString(box.minX) + "," + Double.toHexString(box.minY) + "," + Double.toHexString(box.minZ) + ";"
            + Double.toHexString(box.maxX) + "," + Double.toHexString(box.maxY) + "," + Double.toHexString(box.maxZ);
    }
    private static AABB parse(String id){
        try{
            String[] halves=id.split(";",-1);if(halves.length!=2)return null;
            String[] a=halves[0].split(",",-1),b=halves[1].split(",",-1);if(a.length!=3||b.length!=3)return null;
            return new AABB(Double.valueOf(a[0]),Double.valueOf(a[1]),Double.valueOf(a[2]),Double.valueOf(b[0]),Double.valueOf(b[1]),Double.valueOf(b[2]));
        }catch(RuntimeException ignored){return null;}
    }
}
