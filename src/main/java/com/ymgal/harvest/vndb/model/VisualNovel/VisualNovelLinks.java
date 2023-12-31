package com.ymgal.harvest.vndb.model.VisualNovel;

import com.ymgal.harvest.vndb.model.Common.CommonLinks;
import lombok.Data;

/// <summary>
/// Visual Novel Links
/// </summary>
@Data
public class VisualNovelLinks extends CommonLinks {
    /// <summary>
    /// Encubed Link
    /// </summary>
    private String encubed;
    /// <summary>
    /// Renai Link
    /// </summary>
    private String renai;
}

