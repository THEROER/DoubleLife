package dev.ua.theroer.doublelife.doublelife;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class LuckPermsHandler {

    private final LuckPerms luckPerms;

    public LuckPermsHandler(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public Set<String> getPlayerGroups(UUID playerUuid) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return Set.of();
        }
        return user.getNodes().stream()
            .filter(NodeType.INHERITANCE::matches)
            .map(NodeType.INHERITANCE::cast)
            .map(InheritanceNode::getGroupName)
            .collect(Collectors.toSet());
    }

    public List<String> getPlayerGroupsList(UUID playerUuid) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return List.of();
        }
        return user.getNodes().stream()
            .filter(NodeType.INHERITANCE::matches)
            .map(NodeType.INHERITANCE::cast)
            .map(InheritanceNode::getGroupName)
            .collect(Collectors.toList());
    }

    public void addTemporaryGroup(UUID playerUuid, String groupName, int durationSeconds) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return;
        }
        InheritanceNode.Builder builder = InheritanceNode.builder(groupName);
        if (durationSeconds > 0) {
            builder.expiry(Duration.ofSeconds(durationSeconds));
        }
        Node groupNode = builder.build();
        user.data().add(groupNode);
        luckPerms.getUserManager().saveUser(user);
    }

    public void addTemporaryPermission(UUID playerUuid, String permission, int durationSeconds) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return;
        }
        PermissionNode.Builder builder = PermissionNode.builder(permission);
        if (durationSeconds > 0) {
            builder.expiry(Duration.ofSeconds(durationSeconds));
        }
        Node permNode = builder.build();
        user.data().add(permNode);
        luckPerms.getUserManager().saveUser(user);
    }

    public void removeTemporaryGroup(UUID playerUuid, String groupName) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return;
        }
        user.data().remove(InheritanceNode.builder(groupName).build());
        luckPerms.getUserManager().saveUser(user);
    }

    public void clearTemporaryNodes(UUID playerUuid) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user == null) {
            return;
        }
        user.data().clear(Node::hasExpiry);
        luckPerms.getUserManager().saveUser(user);
    }

    public void createTemporaryGroup(String groupName, int durationSeconds) {
        luckPerms.getGroupManager().createAndLoadGroup(groupName).join();
        if (durationSeconds > 0) {
            // Expiry is handled on assignment; group itself stays temporary by cleanup.
        }
    }

    public void addPermissionsToGroup(String groupName, List<String> permissions, int durationSeconds) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return;
        }
        for (String permission : permissions) {
            PermissionNode.Builder builder = PermissionNode.builder(permission);
            if (durationSeconds > 0) {
                builder.expiry(Duration.ofSeconds(durationSeconds));
            }
            Node permNode = builder.build();
            group.data().add(permNode);
        }
        luckPerms.getGroupManager().saveGroup(group);
    }

    public void deleteGroup(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group != null) {
            luckPerms.getGroupManager().deleteGroup(group).join();
        }
    }
}
