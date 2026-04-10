package com.truper.bonusgenerator.model.dto.mapper;

import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.entity.Commit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommitMapper {
    Commit toEntity(CommitDto commitDto);

    CommitDto toDto(Commit commit);
}