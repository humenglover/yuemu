package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.PageRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.CategoryConstant;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.model.entity.Category;
import com.lumenglover.yuemupicturebackend.model.vo.CategoryVO;
import com.lumenglover.yuemupicturebackend.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/category")
public class CategoryController {
   @Resource
   private CategoryService categoryService;

   /**
    * 分页获取分类列表（管理员）
    * @param pageRequest 分页请求参数
    * @param type 分类类型（1-图片分类，2-帖子分类，3-音频分类）
    * @return 分类列表（包含统计信息）
    */
   @PostMapping("/list/page/vo")
   @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
   public BaseResponse<Page<CategoryVO>> listCategoryVO(PageRequest pageRequest,
                                                        @RequestParam(required = false) Integer type) {
      QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
      if (type != null) {
         queryWrapper.eq("type", type);
      }

      Page<Category> categoryPage = categoryService.page(
              new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize()),
              queryWrapper
      );

      Page<CategoryVO> categoryVOPage = new Page<>(
              pageRequest.getCurrent(),
              pageRequest.getPageSize(),
              categoryPage.getTotal()
      );

      List<CategoryVO> categoryVOList = categoryService.listCategoryVO(categoryPage.getRecords());
      categoryVOPage.setRecords(categoryVOList);
      return ResultUtils.success(categoryVOPage);
   }

   /**
    * 获取指定类型的分类列表
    * @param type 分类类型（1-图片分类，2-帖子分类，3-音频分类）
    * @return 分类名称列表
    */
   @GetMapping("/list/type/{type}")
   public BaseResponse<List<String>> listCategoryByType(@PathVariable Integer type) {
      return ResultUtils.success(categoryService.listCategoryByType(type));
   }

   /**
    * 添加新分类（管理员）
    * @param categoryName 分类名称
    * @param type 分类类型（1-图片分类，2-帖子分类，3-音频分类）
    * @return 添加结果
    */
   @PostMapping("/add")
   @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
   public BaseResponse<Boolean> addCategory(@RequestParam String categoryName,
                                            @RequestParam Integer type) {
      // 校验分类类型是否合法
      if (!Objects.equals(type, CategoryConstant.PICTURE_CATEGORY)
              && !Objects.equals(type, CategoryConstant.POST_CATEGORY)
              && !Objects.equals(type, CategoryConstant.AUDIO_CATEGORY)) {
         return (BaseResponse<Boolean>) ResultUtils.error(40000, "分类类型不合法");
      }
      return ResultUtils.success(categoryService.addCategory(categoryName, type));
   }

   /**
    * 删除分类（管理员）
    * @param categoryId 分类ID
    * @return 删除结果
    */
   @PostMapping("/delete")
   @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
   public BaseResponse<Boolean> deleteCategory(@RequestParam Long categoryId) {
      return ResultUtils.success(categoryService.removeById(categoryId));
   }

   /**
    * 搜索分类（管理员）
    * @param categoryName 分类名称关键词
    * @param type 分类类型（1-图片分类，2-帖子分类，3-音频分类）
    * @return 匹配的分类列表
    */
   @PostMapping("/search")
   @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
   public BaseResponse<List<CategoryVO>> findCategory(@RequestParam String categoryName,
                                                      @RequestParam(required = false) Integer type) {
      return ResultUtils.success(categoryService.findCategory(categoryName, type));
   }
}
