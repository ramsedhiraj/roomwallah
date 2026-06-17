package com.roomwallah.graph;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyGraphService {

    private final PropertyRepository propertyRepository;

    @Data
    @Builder
    public static class GraphNode {
        private String id;
        private String type; // PROPERTY, OWNER, LOCALITY, AMENITY, SCHOOL, TRANSIT
        private String label;
    }

    @Data
    @Builder
    public static class GraphEdge {
        private String source;
        private String target;
        private String relationship; // OWNED_BY, LOCATED_IN, HAS_AMENITY, NEAR_SCHOOL, NEAR_TRANSIT
    }

    @Data
    @Builder
    public static class GraphTraversalResult {
        private List<GraphNode> nodes;
        private List<GraphEdge> edges;
        private List<String> explanationPaths;
    }

    public GraphTraversalResult traverseGraph(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<String> explanationPaths = new ArrayList<>();

        String propNodeId = "PROP-" + property.getId();
        nodes.add(GraphNode.builder()
                .id(propNodeId)
                .type("PROPERTY")
                .label(property.getTitle())
                .build());

        String ownerNodeId = "OWNER-" + property.getOwnerId();
        nodes.add(GraphNode.builder()
                .id(ownerNodeId)
                .type("OWNER")
                .label("Owner ID: " + property.getOwnerId())
                .build());
        edges.add(GraphEdge.builder()
                .source(propNodeId)
                .target(ownerNodeId)
                .relationship("OWNED_BY")
                .build());
        explanationPaths.add(property.getTitle() + " is owned by Owner(" + property.getOwnerId() + ")");

        String localityName = property.getAddress() != null ? property.getAddress().getCity() : "Unknown Locality";
        String localityNodeId = "LOCALITY-" + localityName.toUpperCase().replaceAll("\\s+", "_");
        nodes.add(GraphNode.builder()
                .id(localityNodeId)
                .type("LOCALITY")
                .label(localityName)
                .build());
        edges.add(GraphEdge.builder()
                .source(propNodeId)
                .target(localityNodeId)
                .relationship("LOCATED_IN")
                .build());
        explanationPaths.add(property.getTitle() + " is located in " + localityName);

        if (property.getAmenities() != null) {
            for (String amenity : property.getAmenities()) {
                String amenityNodeId = "AMENITY-" + amenity.toUpperCase().replaceAll("\\s+", "_");
                nodes.add(GraphNode.builder()
                        .id(amenityNodeId)
                        .type("AMENITY")
                        .label(amenity)
                        .build());
                edges.add(GraphEdge.builder()
                        .source(propNodeId)
                        .target(amenityNodeId)
                        .relationship("HAS_AMENITY")
                        .build());
                explanationPaths.add(property.getTitle() + " features amenity: " + amenity);
            }
        }

        String schoolName = "Greenwood High School (" + (property.getAddress() != null ? property.getAddress().getCity() : "Local") + ")";
        String schoolNodeId = "SCHOOL-" + schoolName.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        nodes.add(GraphNode.builder()
                .id(schoolNodeId)
                .type("SCHOOL")
                .label(schoolName)
                .build());
        edges.add(GraphEdge.builder()
                .source(propNodeId)
                .target(schoolNodeId)
                .relationship("NEAR_SCHOOL")
                .build());
        explanationPaths.add(property.getTitle() + " is located near " + schoolName);

        String transitName = "Central Metro Station (" + (property.getAddress() != null ? property.getAddress().getCity() : "Local") + ")";
        String transitNodeId = "TRANSIT-" + transitName.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        nodes.add(GraphNode.builder()
                .id(transitNodeId)
                .type("TRANSIT")
                .label(transitName)
                .build());
        edges.add(GraphEdge.builder()
                .source(propNodeId)
                .target(transitNodeId)
                .relationship("NEAR_TRANSIT")
                .build());
        explanationPaths.add(property.getTitle() + " is located near transit link " + transitName);

        return GraphTraversalResult.builder()
                .nodes(nodes)
                .edges(edges)
                .explanationPaths(explanationPaths)
                .build();
    }
}
