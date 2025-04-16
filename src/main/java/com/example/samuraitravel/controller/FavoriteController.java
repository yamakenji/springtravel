package com.example.samuraitravel.controller;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.Favorite;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.FavoriteService;
import com.example.samuraitravel.service.HouseService;

@Controller
public class FavoriteController {
   private final HouseService houseService;
   private final FavoriteService favoriteService;

   public FavoriteController(HouseService houseService, FavoriteService favoriteService) {
       this.houseService = houseService;
       this.favoriteService = favoriteService;
   }

   @GetMapping("/favorites")
   public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                       @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable,
                       Model model)
   {
       User user = userDetailsImpl.getUser();
       Page<Favorite> favoritePage = favoriteService.findFavoritesByUserOrderByCreatedAtDesc(user, pageable);

       model.addAttribute("favoritePage", favoritePage);

       return "favorites/index";
   }

   @PostMapping("/houses/{houseId}/favorites/create")
   public String create(@PathVariable(name = "houseId") Integer houseId,
                        @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                        RedirectAttributes redirectAttributes,
                        Model model)
   {
       Optional<House> optionalHouse  = houseService.findHouseById(houseId);

       if (optionalHouse.isEmpty()) {
           redirectAttributes.addFlashAttribute("errorMessage", "民宿が存在しません。");

           return "redirect:/houses";
       }

       House house = optionalHouse.get();
       User user = userDetailsImpl.getUser();

       favoriteService.createFavorite(house, user);
       redirectAttributes.addFlashAttribute("successMessage", "お気に入りに追加しました。");

       return "redirect:/houses/{houseId}";
   }

   @PostMapping("/houses/{houseId}/favorites/{favoriteId}/delete")
   public String delete(@PathVariable(name = "houseId") Integer houseId,
                        @PathVariable(name = "favoriteId") Integer favoriteId,
                        @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                        RedirectAttributes redirectAttributes)
   {
       Optional<House> optionalHouse  = houseService.findHouseById(houseId);
       Optional<Favorite> optionalFavorite  = favoriteService.findFavoriteById(favoriteId);

       if (optionalHouse.isEmpty() || optionalFavorite.isEmpty()) {
           redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");

           return "redirect:/houses";
       }

       House house = optionalHouse.get();
       Favorite favorite = optionalFavorite.get();
       User user = userDetailsImpl.getUser();

       if (!favorite.getHouse().equals(house) || !favorite.getUser().equals(user)) {
           redirectAttributes.addFlashAttribute("errorMessage", "不正なアクセスです。");

           return "redirect:/houses/{houseId}";
       }

       favoriteService.deleteFavorite(favorite);
       redirectAttributes.addFlashAttribute("successMessage", "お気に入りを解除しました。");

       return "redirect:/houses/{houseId}";
   }
}