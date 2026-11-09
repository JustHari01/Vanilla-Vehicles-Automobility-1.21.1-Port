package io.github.foundationgames.automobility.automobile.render.obj;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjUtils;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BakedObj(int[] idVertex, int[] idNormal, int[] idUv, float[] vertex, float[] normal, float[] uv,
                       List<Part> parts) {
    public record Part(String name, int[] idVertex, int[] idNormal, int[] idUv) {
    }

    public static BakedObj bake(Obj resource) {
        var obj = ObjUtils.triangulate(resource);
        var vertex = ObjData.getVerticesArray(obj);
        var faces = allFaces(obj);
        var hasNormals = faces.stream().allMatch(ObjFace::containsNormalIndices) && obj.getNumNormals() > 0;
        var normal = hasNormals ? ObjData.getNormalsArray(obj) : generatedNormals(faces, vertex);
        var uv = obj.getNumTexCoords() > 0 ? ObjData.getTexCoordsArray(obj, 2) : new float[]{0, 0};
        var faceIds = new IdentityHashMap<ObjFace, Integer>();
        for (int i = 0; i < faces.size(); i++) {
            faceIds.put(faces.get(i), i);
        }
        var idVertex = vertexIndices(faces);
        var idNormal = normalIndices(faces, faceIds, hasNormals);
        var idUv = uvIndices(faces);
        var groupedFaces = new LinkedHashMap<String, List<ObjFace>>();

        for (int i = 0; i < obj.getNumGroups(); i++) {
            var group = obj.getGroup(i);
            var groupFaces = groupedFaces.computeIfAbsent(group.getName(), ignored -> new ArrayList<>());
            for (int j = 0; j < group.getNumFaces(); j++) {
                groupFaces.add(group.getFace(j));
            }
        }

        var parts = new ArrayList<Part>();
        for (var entry : groupedFaces.entrySet()) {
            var groupFaces = entry.getValue();
            var partVertex = vertexIndices(groupFaces);
            var partNormal = normalIndices(groupFaces, faceIds, hasNormals);
            var partUv = uvIndices(groupFaces);
            if (partVertex.length > 0) {
                parts.add(new Part(entry.getKey(), partVertex, partNormal, partUv));
            }
        }

        if (parts.isEmpty()) {
            parts.add(new Part("default", idVertex, idNormal, idUv));
        }

        return new BakedObj(
                idVertex,
                idNormal,
                idUv,
                vertex,
                normal,
                uv,
                List.copyOf(parts)
        );
    }

    private static List<ObjFace> allFaces(Obj obj) {
        var faces = new ArrayList<ObjFace>(obj.getNumFaces());
        for (int i = 0; i < obj.getNumFaces(); i++) {
            faces.add(obj.getFace(i));
        }
        return faces;
    }

    private static int[] vertexIndices(List<ObjFace> faces) {
        var result = new int[faces.stream().mapToInt(ObjFace::getNumVertices).sum()];
        int offset = 0;
        for (var face : faces) {
            for (int i = 0; i < face.getNumVertices(); i++) {
                result[offset++] = face.getVertexIndex(i);
            }
        }
        return result;
    }

    private static int[] normalIndices(List<ObjFace> faces, Map<ObjFace, Integer> faceIds, boolean hasNormals) {
        var result = new int[faces.stream().mapToInt(ObjFace::getNumVertices).sum()];
        int offset = 0;
        for (var face : faces) {
            for (int i = 0; i < face.getNumVertices(); i++) {
                result[offset++] = hasNormals ? face.getNormalIndex(i) : faceIds.get(face);
            }
        }
        return result;
    }

    private static int[] uvIndices(List<ObjFace> faces) {
        var result = new int[faces.stream().mapToInt(ObjFace::getNumVertices).sum()];
        int offset = 0;
        for (var face : faces) {
            for (int i = 0; i < face.getNumVertices(); i++) {
                result[offset++] = face.containsTexCoordIndices() ? face.getTexCoordIndex(i) : 0;
            }
        }
        return result;
    }

    private static float[] generatedNormals(List<ObjFace> faces, float[] vertex) {
        var result = new float[faces.size() * 3];
        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            var face = faces.get(faceIndex);
            float nx = 0;
            float ny = 1;
            float nz = 0;
            if (face.getNumVertices() >= 3) {
                int a = face.getVertexIndex(0) * 3;
                int b = face.getVertexIndex(1) * 3;
                int c = face.getVertexIndex(2) * 3;
                float abx = vertex[b] - vertex[a];
                float aby = vertex[b + 1] - vertex[a + 1];
                float abz = vertex[b + 2] - vertex[a + 2];
                float acx = vertex[c] - vertex[a];
                float acy = vertex[c + 1] - vertex[a + 1];
                float acz = vertex[c + 2] - vertex[a + 2];
                nx = aby * acz - abz * acy;
                ny = abz * acx - abx * acz;
                nz = abx * acy - aby * acx;
                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0.000001f) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                } else {
                    nx = 0;
                    ny = 1;
                    nz = 0;
                }
            }
            int n = faceIndex * 3;
            result[n] = nx;
            result[n + 1] = ny;
            result[n + 2] = nz;
        }
        return result;
    }
}
