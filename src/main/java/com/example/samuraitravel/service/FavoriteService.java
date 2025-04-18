package com.example.samuraitravel.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.samuraitravel.entity.Favorite;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.repository.FavoriteRepository;

@Service
public class FavoriteService {
   private final FavoriteRepository favoriteRepository;

   public FavoriteService(FavoriteRepository favoriteRepository) {
       this.favoriteRepository = favoriteRepository;
   }

   // 指定したidを持つお気に入りを取得する
   public Optional<Favorite> findFavoriteById(Integer id) {
       return favoriteRepository.findById(id);
   }

   // 指定した民宿とユーザーのお気に入りを取得する
   public Favorite findFavoriteByHouseAndUser(House house, User user) {
       return favoriteRepository.findByHouseAndUser(house, user);
   }

   // 指定したユーザーのすべてのお気に入りを作成日時が新しい順に並べ替え、ページングされた状態で取得する
   public Page<Favorite> findFavoritesByUserOrderByCreatedAtDesc(User user, Pageable pageable) {
       return favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);
   }

   @Transactional
   public void createFavorite(House house, User user) {
       Favorite favorite = new Favorite();

       favorite.setHouse(house);
       favorite.setUser(user);

       favoriteRepository.save(favorite);
   }

   @Transactional
   public void deleteFavorite(Favorite favorite) {
       favoriteRepository.delete(favorite);
   }

   // 指定したユーザーが指定した民宿をすでにお気に入りに追加済みかどうかをチェックする
   public boolean isFavorite(House house, User user) {
       return favoriteRepository.findByHouseAndUser(house, user) != null;
   }
}