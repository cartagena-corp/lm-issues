package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.IssueDtoRequest;
import com.cartagenacorp.lm_issues.dto.IssueDtoResponse;
import com.cartagenacorp.lm_issues.dto.IssueRelationDto;
import com.cartagenacorp.lm_issues.dto.UserBasicDataDto;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.entity.IssueRelation;
import com.cartagenacorp.lm_issues.exceptions.BaseException;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.mapper.IssueRelationMapper;
import com.cartagenacorp.lm_issues.repository.IssueRelationRepository;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import com.cartagenacorp.lm_issues.util.JwtContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueRelationService {

    private final IssueRepository issueRepository;
    private final IssueRelationRepository issueRelationRepository;
    private final IssueMapper issueMapper;
    private final IssueRelationMapper issueRelationMapper;
    private final UserExternalService userExternalService;
    private final AuditExternalService auditExternalService;
    private final NotificationExternalService notificationExternalService;

    public IssueRelationService(IssueRepository issueRepository, IssueRelationRepository issueRelationRepository, IssueMapper issueMapper, IssueRelationMapper issueRelationMapper,
                                UserExternalService userExternalService, AuditExternalService auditExternalService, NotificationExternalService notificationExternalService) {
        this.issueRepository = issueRepository;
        this.issueRelationRepository = issueRelationRepository;
        this.issueMapper = issueMapper;
        this.issueRelationMapper = issueRelationMapper;
        this.userExternalService = userExternalService;
        this.auditExternalService = auditExternalService;
        this.notificationExternalService = notificationExternalService;
    }

    @Transactional
    public IssueDtoResponse createSubtask(UUID parentId, IssueDtoRequest subtask) {
        Issue parent = issueRepository.findById(parentId)
                .orElseThrow(() -> new BaseException("No se encontró el Issue principal", HttpStatus.NOT_FOUND.value()));

        Issue subtaskEntity = issueMapper.toEntity(subtask);
        subtaskEntity.setParent(parent);
        subtaskEntity.setProjectId(parent.getProjectId());
        subtaskEntity.setReporterId(JwtContextHolder.getUserId());
        subtaskEntity.setOrganizationId(parent.getOrganizationId());

        return issueMapper.toDto(issueRepository.save(subtaskEntity));
    }

    @Transactional(readOnly = true)
    public List<IssueDtoResponse> getSubtasks(UUID parentId) {
        List<Issue> subtasks = issueRepository.findByParentId(parentId);

        if (subtasks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> userIds = new HashSet<>();
        subtasks.forEach(subtask -> {
            if (subtask.getAssignedId() != null) userIds.add(subtask.getAssignedId());
            if (subtask.getReporterId() != null) userIds.add(subtask.getReporterId());
        });

        List<UserBasicDataDto> usersOpt;
        try {
            usersOpt = userExternalService.getUsersData(
                    JwtContextHolder.getToken(),
                    userIds.stream().map(UUID::toString).toList()
            );
        } catch (Exception e) {
            usersOpt = Collections.emptyList();
        }

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        return subtasks.stream()
                .map(issue -> getIssueDtoResponse(userMap, issue))
                .toList();
    }

    private IssueDtoResponse getIssueDtoResponse(Map<UUID, UserBasicDataDto> userMap, Issue issue) {
        IssueDtoResponse issueDtoResponse = issueMapper.toDto(issue);

        issueDtoResponse.setReporterId(userMap.getOrDefault(issue.getReporterId(),
                new UserBasicDataDto(issue.getReporterId(), null, null, null, null, null)));


        issueDtoResponse.setAssignedId(userMap.getOrDefault(issue.getAssignedId(),
                new UserBasicDataDto(issue.getAssignedId(), null, null, null, null, null)));

        return issueDtoResponse;
    }

    private IssueDtoResponse getIssueDtoResponse(Issue issue) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(issue.getReporterId());
        if (issue.getAssignedId() != null) { userIds.add(issue.getAssignedId());}

        List<UserBasicDataDto> usersOpt = userExternalService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream().map(UUID::toString).toList()
        );

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        return getIssueDtoResponse(userMap, issue);
    }

    @Transactional
    public void relateIssues(UUID sourceId, UUID targetId) {
        Issue source = issueRepository.findById(sourceId)
                .orElseThrow(() -> new BaseException("Issue origen no encontrado", HttpStatus.NOT_FOUND.value()));
        Issue target = issueRepository.findById(targetId)
                .orElseThrow(() -> new BaseException("Issue destino no encontrado", HttpStatus.NOT_FOUND.value()));

        boolean alreadyRelated = issueRelationRepository.findBySource_Id(sourceId)
                .stream()
                .anyMatch(r -> r.getTarget().getId().equals(targetId));

        if (alreadyRelated) {
            throw new BaseException("Los Issues ya están relacionados", HttpStatus.CONFLICT.value());
        }

        IssueRelation relation = new IssueRelation();
        relation.setSource(source);
        relation.setTarget(target);
        issueRelationRepository.save(relation);
    }

    @Transactional
    public void unrelateIssues(UUID sourceId, UUID targetId) {
        IssueRelation relation = issueRelationRepository.findBySource_Id(sourceId)
                .stream()
                .filter(r -> r.getTarget().getId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new BaseException("Relación no encontrada", HttpStatus.NOT_FOUND.value()));
        issueRelationRepository.delete(relation);
    }

    @Transactional
    public List<IssueRelationDto> getRelatedIssues(UUID issueId) {
        return issueRelationRepository.findBySource_Id(issueId)
                .stream()
                .map(issueRelationMapper::toDto)
                .toList();
    }

    @Transactional
    public List<IssueRelationDto> getIssuesThatRelateTo(UUID issueId) {
        return issueRelationRepository.findByTarget_Id(issueId)
                .stream()
                .map(issueRelationMapper::toDto)
                .toList();
    }
}
