<template>
  <div>
    <div v-if="!isAuthenticated" class="login-wrapper">
      <div class="login-card">
        <div class="login-icon">🔒</div>
        <h2>Admin Access</h2>
        <p>Please log in to view the dashboard.</p>

        <form @submit.prevent="handleLogin" class="login-form">
          <input
            type="text"
            v-model="username"
            placeholder="Username"
            required
            class="login-input"
          />
          <input
            type="password"
            v-model="password"
            placeholder="Password"
            required
            class="login-input"
          />

          <p v-if="loginError" class="error-text">{{ loginError }}</p>

          <button type="submit" class="login-btn" :disabled="isLoggingIn">
            {{ isLoggingIn ? 'Authenticating...' : 'Sign In' }}
          </button>

          <a href="/" class="login-return-btn">
            &larr; Return to Public Survey
          </a>
        </form>
      </div>
    </div>

    <div v-else class="dashboard-layout">
      <header class="top-header">
        <div class="header-left">
          <div class="logo-icon">📊</div>
          <div>
            <h1>Food Preferences Survey</h1>
            <p class="subtitle">
              {{ dynamicQuestions.length }} Questions • {{ menuItems.length }} Items
            </p>
          </div>
        </div>
        <div class="header-right">
          <span class="live-badge"><span class="dot"></span> Live</span>

          <a href="/" class="view-survey-btn" title="View Public Survey">
            🍴 Public Survey
          </a>

          <button class="logout-btn" @click="showLogoutModal = true">🚪 Log Out</button>

          <button class="danger-btn" @click="showClearModal = true">🗑️ Clear Data</button>

          <button class="export-btn" @click="downloadReport">📥 Export Report</button>
        </div>
      </header>

      <div class="admin-tabs-nav-bar">
        <div class="admin-tabs-container">
          <div class="sliding-highlight" :class="activeAdminTab"></div>

          <button
            class="tab-btn"
            :class="{ active: activeAdminTab === 'analytics' }"
            @click="activeAdminTab = 'analytics'"
          >
            <span class="tab-btn-icon">📊</span>
            <span class="tab-btn-text">Analytics View</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeAdminTab === 'manager' }"
            @click="activeAdminTab = 'manager'"
          >
            <span class="tab-btn-icon">⚙️</span>
            <span class="tab-btn-text">Menu Manager</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeAdminTab === 'descriptions' }"
            @click="activeAdminTab = 'descriptions'"
          >
            <span class="tab-btn-icon">✎</span>
            <span class="tab-btn-text">Item Descriptions</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeAdminTab === 'questions' }"
            @click="activeAdminTab = 'questions'"
          >
            <span class="tab-btn-icon">❓</span>
            <span class="tab-btn-text">Question Manager</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeAdminTab === 'statistics' }"
            @click="activeAdminTab = 'statistics'"
          >
            <span class="tab-btn-icon">📐</span>
            <span class="tab-btn-text">Statistical Tests</span>
          </button>
        </div>
      </div>

      <div v-show="activeAdminTab === 'analytics'">
        <section class="kpi-grid">
          <div class="new-kpi-card global-card">
            <div class="scope-label"><span class="scope-dot blue-dot"></span> GLOBAL</div>
            <h2 class="kpi-val">{{ demographics.globalParticipants || baselineCount }}</h2>
            <p class="kpi-name">PARTICIPANTS</p>
            <p class="kpi-desc">
              Target {{ globalParticipantTarget }} •
              {{
                (demographics.globalParticipants || baselineCount) >= globalParticipantTarget
                  ? 'Goal Reached!'
                  : `Need ${globalParticipantTarget - (demographics.globalParticipants || baselineCount)} more`
              }}
            </p>
          </div>

          <div class="new-kpi-card item-card">
            <div class="scope-label"><span class="scope-dot orange-dot"></span> CURRENT ITEM</div>
            <h2 class="kpi-val">{{ itemTotal }}</h2>
            <p class="kpi-name">TOTAL RESPONSES</p>
            <p class="kpi-desc">{{ itemCoverageLabel }}</p>
            <p class="kpi-desc">
              Target {{ SURVEY_BASELINE_TARGET }} per item •
              {{ itemTotal >= SURVEY_BASELINE_TARGET ? 'Goal Reached!' : `Need ${SURVEY_BASELINE_TARGET - itemTotal} more` }}
            </p>
          </div>

          <div class="new-kpi-card item-card">
            <div class="scope-label"><span class="scope-dot orange-dot"></span> CURRENT ITEM</div>
            <h2 class="kpi-val">{{ sentiment.posPct }}%</h2>
            <p class="kpi-name">SUITABILITY RATE</p>
            <p class="kpi-desc">Ratings 4-5 (Suitable / High)</p>
          </div>

          <div class="new-kpi-card item-card">
            <div class="scope-label"><span class="scope-dot orange-dot"></span> CURRENT ITEM</div>
            <h2 class="kpi-val">{{ sentiment.neuPct }}%</h2>
            <p class="kpi-name">NEUTRAL</p>
            <p class="kpi-desc">Rating 3 (Moderate)</p>
          </div>

          <div class="new-kpi-card item-card">
            <div class="scope-label"><span class="scope-dot orange-dot"></span> CURRENT ITEM</div>
            <h2 class="kpi-val">{{ sentiment.negPct }}%</h2>
            <p class="kpi-name">NEEDS ATTENTION</p>
            <p class="kpi-desc">Ratings 1-2 (Low suitability)</p>
          </div>
        </section>

        <!-- SECTION 1: DEMOGRAPHIC OVERVIEW -->
        <section class="demographics-overview-panel">
          <div class="demo-panel-header">
            <div class="demo-panel-title-wrap">
              <span class="scope-dot blue-dot"></span>
              <div>
                <h3>SECTION 1 — Survey Respondent Profile</h3>
                <p class="demo-panel-sub">
                  Overall demographic distribution across all survey takers (Total: {{ demographics.globalParticipants || baselineCount }} respondent{{ (demographics.globalParticipants || baselineCount) === 1 ? '' : 's' }})
                </p>
              </div>
            </div>
          </div>

          <div class="demo-panel-content">
            <div class="demo-stat-card">
              <div class="demo-card-top">
                <span class="demo-icon">🎂</span>
                <div>
                  <h4>Age Group Distribution</h4>
                  <p class="demo-hint">Participant age range</p>
                </div>
              </div>
              <div class="demo-bars-list">
                <div v-for="item in ageGroupStats" :key="item.label" class="demo-bar-item">
                  <div class="demo-bar-meta">
                    <span class="demo-opt-label">{{ item.label }}</span>
                    <span class="demo-opt-val"><strong>{{ item.count }}</strong> ({{ item.pct }}%)</span>
                  </div>
                  <div class="demo-track">
                    <div class="demo-fill blue-theme" :style="{ width: item.pct + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="demo-stat-card">
              <div class="demo-card-top">
                <span class="demo-icon">🍽️</span>
                <div>
                  <h4>Dining Frequency Distribution</h4>
                  <p class="demo-hint">How often participants dine at cafés / restaurants</p>
                </div>
              </div>
              <div class="demo-bars-list">
                <div v-for="item in diningFrequencyStats" :key="item.label" class="demo-bar-item">
                  <div class="demo-bar-meta">
                    <span class="demo-opt-label">{{ item.label }}</span>
                    <span class="demo-opt-val"><strong>{{ item.count }}</strong> ({{ item.pct }}%)</span>
                  </div>
                  <div class="demo-track">
                    <div class="demo-fill blue-theme" :style="{ width: item.pct + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="navigation-panel" v-if="menuItems.length > 0">
          <div class="filters-row">
            <div class="category-toggle">
              <div
                class="sliding-bg"
                :class="activeCategory === 'Beverages' ? 'slide-right' : 'slide-left'"
              ></div>

              <button :class="{ active: activeCategory === 'Meals' }" @click="setCategory('Meals')">
                🍽️ Meals
              </button>
              <button
                :class="{ active: activeCategory === 'Beverages' }"
                @click="setCategory('Beverages')"
              >
                🥤 Beverages
              </button>
            </div>

            <div class="divider"></div>

            <div class="subcategory-pills">
              <button
                class="f-pill pill-all"
                :class="{ active: activeSubcategory === 'All' }"
                @click="setSubcategory('All')"
              >
                All {{ activeCategory }}
              </button>

              <button
                v-for="sub in currentSubcategories"
                :key="sub"
                class="f-pill"
                :class="[getPillClass(sub), { active: activeSubcategory === sub }]"
                @click="setSubcategory(sub)"
              >
                {{ sub }}
              </button>
            </div>
          </div>

          <div class="item-tabs-container">
            <div v-if="menuLoadError" class="empty-filter" role="alert">
              Could not load the menu. <button type="button" @click="fetchMenuItems">Try again</button>
            </div>
            <div v-else-if="filteredMenuItems.length === 0" class="empty-filter">
              No items found in this category.
            </div>

            <div
              v-for="item in filteredMenuItems"
              :key="item.id"
              class="item-tab"
              :class="{ active: selectedItemId === item.id }"
              role="button"
              tabindex="0"
              :aria-label="`View analytics for ${item.name}`"
              :aria-pressed="selectedItemId === item.id"
              @click="selectItem(item.id)"
              @keydown.enter.prevent="selectItem(item.id)"
              @keydown.space.prevent="selectItem(item.id)"
            >
              <div
                class="tab-thumb"
                :style="item?.imageName ? { backgroundImage: `url('${getImagePath(item)}')` } : {}"
              ></div>

              <span class="tab-title truncate-text">{{ item.name }}</span>
              <span class="tab-cat">{{ item.category }}</span>
            </div>
          </div>
        </section>

        <div v-if="isLoading" class="state-message">
          <h2>Loading analytics for {{ menuItem?.name }}...</h2>
        </div>
        <div v-else-if="analyticsLoadError" class="state-message" role="alert">
          <h2>Could not load analytics for {{ menuItem?.name }}.</h2>
          <button type="button" class="nav-btn primary" @click="selectedItemId != null && fetchCombinedAnalyticsForItem(selectedItemId)">Try again</button>
        </div>
        <div v-else-if="!menuItem" class="state-message">
          <h2>Select an item to view analytics.</h2>
        </div>
        <div v-else-if="itemTotal === 0" class="state-message empty">
          <h2>No Data Yet 📭</h2>
          <p>
            Nobody has submitted a survey for <strong>{{ menuItem?.name }}</strong> yet. Check back
            later!
          </p>
        </div>

        <div v-else class="item-analytics-view">
          <!-- Item Summary Header Card -->
          <div class="item-summary-card" :style="getCategoryStyles(menuItem?.category)">
            <div class="summary-left">
              <div
                class="summary-thumb"
                :style="menuItem?.imageName ? { backgroundImage: `url('${getImagePath(menuItem)}')` } : {}"
              ></div>
              <div class="summary-details">
                <div class="summary-badges">
                  <span class="badge-v2 food-badge" :class="getPillClass(menuItem?.category)">
                    🍴 {{ menuItem?.category }}
                  </span>
                  <span class="badge-v2 eval-badge">
                    👥 {{ itemTotal }} Evaluation{{ itemTotal === 1 ? '' : 's' }}
                  </span>
                </div>
                <h2 class="summary-title">{{ menuItem?.name }}</h2>
                <p v-if="getItemDescription(menuItem)" class="summary-desc">{{ getItemDescription(menuItem) }}</p>
              </div>
            </div>
            <div class="summary-metrics">
              <div class="metric-pill-box">
                <span class="metric-num">{{ avgSuitabilityScore ? `${avgSuitabilityScore.toFixed(1)} ★` : `${(sentiment.posPct / 20).toFixed(1)} ★` }}</span>
                <span class="metric-lbl">Avg Suitability</span>
              </div>
              <div class="metric-pill-box">
                <span class="metric-num">{{ displayMoodData.topRowLabel || '-' }}</span>
                <span class="metric-lbl">Top Mood</span>
              </div>
              <div class="metric-pill-box">
                <span class="metric-num">{{ displayWeatherData.topRowLabel || '-' }}</span>
                <span class="metric-lbl">Top Weather</span>
              </div>
            </div>
          </div>

          <!-- SECTION 2: GRID EVALUATION CARDS (QUESTION 1 & QUESTION 2) -->
          <div class="section2-grid-columns">
            <!-- Question 1 — Mood Association Card -->
            <div class="grid-card-container" :style="getCategoryStyles(menuItem?.category)">
              <div class="grid-card-head">
                <div class="head-title-wrap">
                  <span class="q-badge">Q1</span>
                  <div>
                    <h3>Question 1 — Mood Association</h3>
                    <p class="grid-card-subtitle">
                      How suitable is <strong>{{ menuItem?.name }}</strong> for each mood? (Scale 1–5)
                    </p>
                  </div>
                </div>
                <span class="evaluator-pill">
                  👥 {{ displayMoodData.totalEvaluators || itemTotal }} Evaluator{{ (displayMoodData.totalEvaluators || itemTotal) === 1 ? '' : 's' }}
                </span>
              </div>

              <div class="grid-card-content">
                <div class="grid-col-headers">
                  <span class="hdr-label">Mood & Emotion</span>
                  <span class="hdr-score">Avg Score</span>
                  <span class="hdr-bar">Score Track & Vote Spread</span>
                  <span class="hdr-suit">% Suitable</span>
                  <span class="hdr-votes">Votes</span>
                </div>

                <div class="grid-rows-list">
                  <div
                    v-for="row in displayMoodData.rows"
                    :key="row.id"
                    class="grid-row-item"
                    :class="{ 'leader-row': row.shortLabel === displayMoodData.topRowLabel }"
                  >
                    <div class="row-info-col">
                      <div class="row-mood-title">
                        <strong>{{ row.shortLabel }}</strong>
                        <span v-if="row.shortLabel === displayMoodData.topRowLabel" class="top-tag">⭐ Top</span>
                      </div>
                      <span class="row-mood-desc">{{ getMoodContext(row.label, row.shortLabel) }}</span>
                    </div>

                    <div class="row-score-col">
                      <span class="score-chip" :class="getScoreBadgeClass(row.avgRating)">
                        {{ row.avgRating > 0 ? `${row.avgRating.toFixed(1)} ★` : '—' }}
                      </span>
                    </div>

                    <div class="row-bar-col">
                      <div class="rating-track-bar">
                        <div
                          class="rating-track-fill"
                          :style="{ width: Math.min(100, (row.avgRating / 5) * 100) + '%' }"
                        ></div>
                      </div>
                      <div class="mini-scale-ticks dist-ticks" :title="distTitle(row)">
                        <span
                          v-for="n in [1, 2, 3, 4, 5]"
                          :key="n"
                          :class="{ 'dist-zero': !rowVoteCount(row, n) }"
                        >{{ n }}:{{ rowVoteCount(row, n) }}</span>
                      </div>
                    </div>

                    <div class="row-suit-col">
                      <span
                        class="suit-badge"
                        :class="row.suitabilityPct >= 70 ? 'suit-high' : row.suitabilityPct >= 40 ? 'suit-med' : 'suit-low'"
                      >
                        {{ row.suitabilityPct }}%
                      </span>
                    </div>

                    <div class="row-votes-col">
                      <span class="votes-badge">{{ row.totalVotes }}</span>
                    </div>
                  </div>
                </div>

                <div class="grid-insight-footer" v-if="displayMoodData.topRowLabel">
                  <span class="insight-lamp">💡</span>
                  <span class="insight-text">
                    <strong>Key Insight:</strong> <strong>{{ displayMoodData.topRowLabel }}</strong> is the leading mood association with an average suitability score of <strong>{{ displayMoodData.topRowScore.toFixed(1) }} / 5.0</strong>.
                  </span>
                </div>
              </div>
            </div>

            <!-- Question 2 — Weather Association Card -->
            <div class="grid-card-container" :style="getCategoryStyles(menuItem?.category)">
              <div class="grid-card-head">
                <div class="head-title-wrap">
                  <span class="q-badge">Q2</span>
                  <div>
                    <h3>Question 2 — Weather Association</h3>
                    <p class="grid-card-subtitle">
                      How suitable is <strong>{{ menuItem?.name }}</strong> for each weather condition? (Scale 1–5)
                    </p>
                  </div>
                </div>
                <span class="evaluator-pill">
                  ⛅ {{ displayWeatherData.totalEvaluators || itemTotal }} Evaluator{{ (displayWeatherData.totalEvaluators || itemTotal) === 1 ? '' : 's' }}
                </span>
              </div>

              <div class="grid-card-content">
                <div class="grid-col-headers">
                  <span class="hdr-label">Weather Condition</span>
                  <span class="hdr-score">Avg Score</span>
                  <span class="hdr-bar">Score Track & Vote Spread</span>
                  <span class="hdr-suit">% Suitable</span>
                  <span class="hdr-votes">Votes</span>
                </div>

                <div class="grid-rows-list">
                  <div
                    v-for="row in displayWeatherData.rows"
                    :key="row.id"
                    class="grid-row-item"
                    :class="{ 'leader-row': row.shortLabel === displayWeatherData.topRowLabel }"
                  >
                    <div class="row-info-col">
                      <div class="row-mood-title">
                        <strong>{{ getWeatherIcon(row.shortLabel) }} {{ row.shortLabel }}</strong>
                        <span v-if="row.shortLabel === displayWeatherData.topRowLabel" class="top-tag">⭐ Top</span>
                      </div>
                      <span class="row-mood-desc" v-if="row.id === 'cool_dry'">Breezy, air-conditioned, cool evening weather</span>
                      <span class="row-mood-desc" v-else>{{ row.shortLabel }} weather condition</span>
                    </div>

                    <div class="row-score-col">
                      <span class="score-chip" :class="getScoreBadgeClass(row.avgRating)">
                        {{ row.avgRating > 0 ? `${row.avgRating.toFixed(1)} ★` : '—' }}
                      </span>
                    </div>

                    <div class="row-bar-col">
                      <div class="rating-track-bar">
                        <div
                          class="rating-track-fill"
                          :style="{ width: Math.min(100, (row.avgRating / 5) * 100) + '%' }"
                        ></div>
                      </div>
                      <div class="mini-scale-ticks dist-ticks" :title="distTitle(row)">
                        <span
                          v-for="n in [1, 2, 3, 4, 5]"
                          :key="n"
                          :class="{ 'dist-zero': !rowVoteCount(row, n) }"
                        >{{ n }}:{{ rowVoteCount(row, n) }}</span>
                      </div>
                    </div>

                    <div class="row-suit-col">
                      <span
                        class="suit-badge"
                        :class="row.suitabilityPct >= 70 ? 'suit-high' : row.suitabilityPct >= 40 ? 'suit-med' : 'suit-low'"
                      >
                        {{ row.suitabilityPct }}%
                      </span>
                    </div>

                    <div class="row-votes-col">
                      <span class="votes-badge">{{ row.totalVotes }}</span>
                    </div>
                  </div>
                </div>

                <div class="grid-insight-footer" v-if="displayWeatherData.topRowLabel">
                  <span class="insight-lamp">💡</span>
                  <span class="insight-text">
                    <strong>Key Insight:</strong> <strong>{{ displayWeatherData.topRowLabel }}</strong> is the optimal weather condition with an average score of <strong>{{ displayWeatherData.topRowScore.toFixed(1) }} / 5.0</strong>.
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- INDIVIDUAL RESPONSES LOG (SURVEY TAKER DATA) -->
          <div class="responses-log-section">
            <div class="log-section-header">
              <div class="log-title-area">
                <span class="log-emoji">📋</span>
                <div>
                  <h3>Survey Takers — Individual Evaluations</h3>
                  <p class="log-desc">Complete rating details and feedback submitted for <strong>{{ menuItem?.name }}</strong></p>
                </div>
              </div>
              <span class="log-badge">{{ displayResponses.length }} Submission{{ displayResponses.length === 1 ? '' : 's' }}</span>
            </div>

            <div v-if="displayResponses.length === 0" class="log-empty">
              <p>No individual evaluation breakdown recorded yet.</p>
            </div>

            <div v-else class="log-cards-grid">
              <div v-for="(resp, idx) in displayResponses" :key="resp.userId || idx" class="submission-card">
                <div class="sub-card-header">
                  <div class="respondent-info">
                    <span class="user-avatar">👤</span>
                    <div>
                      <h4 class="respondent-title">Respondent #{{ idx + 1 }}</h4>
                      <span class="respondent-sub" :title="resp.userId">{{ formatUserId(resp.userId) }}</span>
                    </div>
                  </div>
                </div>

                <div class="sub-card-body">
                  <!-- Mood Ratings -->
                  <div class="sub-group" v-if="hasRatings(resp.moodRatings)">
                    <p class="sub-group-title">Question 1 — Mood Ratings</p>
                    <div class="rating-pills-wrap">
                      <span
                        v-for="(val, mood) in resp.moodRatings"
                        :key="mood"
                        class="eval-pill"
                        :class="getRatingColorClass(val)"
                      >
                        {{ mood }}: <strong>{{ val }}★</strong>
                      </span>
                    </div>
                  </div>

                  <!-- Weather Ratings -->
                  <div class="sub-group" v-if="hasRatings(resp.weatherRatings)">
                    <p class="sub-group-title">Question 2 — Weather Ratings</p>
                    <div class="rating-pills-wrap">
                      <span
                        v-for="(val, weather) in resp.weatherRatings"
                        :key="weather"
                        class="eval-pill weather"
                        :class="getRatingColorClass(val)"
                      >
                        {{ weather }}: <strong>{{ val }}★</strong>
                      </span>
                    </div>
                  </div>

                  <!-- Text Feedback if any -->
                  <div class="sub-group text-group" v-if="resp.textFeedback">
                    <p class="sub-group-title">Written Review</p>
                    <p class="user-text-quote">"{{ resp.textFeedback }}"</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <div v-if="activeAdminTab === 'manager'" class="manager-layout fade-in">
        <div class="manager-header-row item-manager-header">
          <div>
            <h2>Menu Item Database</h2>
            <p class="manager-description">Keep the live survey menu accurate and easy to rate.</p>
          </div>
          <div class="manager-header-actions">
            <button
              class="nav-btn danger-outline"
              :disabled="menuItems.length === 0 || isDeletingAll"
              @click="showDeleteAllModal = true"
            >
              {{ isDeletingAll ? 'Deleting...' : 'Delete All' }}
            </button>
            <button class="nav-btn orange-solid add-item-btn" @click="openNewItemModal">
              <span aria-hidden="true">+</span> Add New Item
            </button>
          </div>
        </div>

        <div class="manager-toolbar" role="search">
          <label class="search-field">
            <span aria-hidden="true">⌕</span>
            <span class="sr-only">Search menu items</span>
            <input
              v-model="itemSearch"
              type="search"
              placeholder="Search by item name or image file"
            />
            <button
              v-if="itemSearch"
              type="button"
              class="clear-search"
              aria-label="Clear search"
              @click="itemSearch = ''"
            >
              ×
            </button>
          </label>
          <div
            ref="categoryDropdownRef"
            class="custom-category-dropdown"
            @keydown="handleCategoryDropdownKeydown"
          >
            <button
              type="button"
              class="cat-dropdown-trigger"
              :class="{ open: isCategoryDropdownOpen, active: itemCategoryFilter !== 'All' }"
              aria-haspopup="listbox"
              :aria-expanded="isCategoryDropdownOpen"
              aria-label="Filter menu items by category"
              @click="toggleCategoryDropdown"
            >
              <div class="trigger-content">
                <span class="trigger-label">Category:</span>
                <span v-if="itemCategoryFilter === 'All'" class="trigger-badge all-badge">
                  <span class="filter-tag-icon">🏷️</span> All Categories
                  <span class="count-pill">{{ menuItems.length }}</span>
                </span>
                <span
                  v-else
                  class="f-pill selected-cat-pill"
                  :class="getPillClass(itemCategoryFilter)"
                >
                  {{ itemCategoryFilter }}
                  <span class="count-pill">{{ getCategoryCount(itemCategoryFilter) }}</span>
                </span>
              </div>
              <button
                v-if="itemCategoryFilter !== 'All'"
                type="button"
                class="quick-clear-cat"
                title="Reset to all categories"
                aria-label="Reset category filter"
                @click.stop="selectCategoryFilter('All')"
              >
                ✕
              </button>
              <span
                class="chevron-icon"
                :class="{ rotated: isCategoryDropdownOpen }"
                aria-hidden="true"
              >
                <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
                  <path
                    fill-rule="evenodd"
                    d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                    clip-rule="evenodd"
                  />
                </svg>
              </span>
            </button>

            <Transition name="dropdown-pop">
              <div
                v-if="isCategoryDropdownOpen"
                class="cat-dropdown-menu"
                role="listbox"
                aria-label="Categories"
              >
                <!-- Option: All Categories -->
                <button
                  type="button"
                  class="cat-menu-item all-option"
                  :class="{ selected: itemCategoryFilter === 'All' }"
                  role="option"
                  :aria-selected="itemCategoryFilter === 'All'"
                  @click="selectCategoryFilter('All')"
                >
                  <div class="item-left">
                    <span class="all-icon">🏷️</span>
                    <span class="item-name">All Categories</span>
                  </div>
                  <div class="item-right">
                    <span class="item-count-badge">{{ menuItems.length }}</span>
                    <span v-if="itemCategoryFilter === 'All'" class="check-mark">✓</span>
                  </div>
                </button>

                <div class="menu-divider"></div>

                <!-- Section 1: Meals -->
                <div class="menu-section">
                  <div class="section-title-row">
                    <span class="section-badge-icon">🍽️</span>
                    <span class="section-heading">MEALS</span>
                    <span class="section-count">({{ mealsCount }})</span>
                  </div>
                  <div class="section-items">
                    <button
                      v-for="cat in foodSubcategories"
                      :key="cat"
                      type="button"
                      class="cat-menu-item"
                      :class="{ selected: itemCategoryFilter === cat }"
                      role="option"
                      :aria-selected="itemCategoryFilter === cat"
                      @click="selectCategoryFilter(cat)"
                    >
                      <div class="item-left">
                        <span class="f-pill cat-preview-pill" :class="getPillClass(cat)">
                          {{ cat }}
                        </span>
                      </div>
                      <div class="item-right">
                        <span class="item-count-badge">{{ getCategoryCount(cat) }}</span>
                        <span v-if="itemCategoryFilter === cat" class="check-mark">✓</span>
                      </div>
                    </button>
                  </div>
                </div>

                <div class="menu-divider"></div>

                <!-- Section 2: Beverages -->
                <div class="menu-section">
                  <div class="section-title-row">
                    <span class="section-badge-icon">🥤</span>
                    <span class="section-heading">BEVERAGES</span>
                    <span class="section-count">({{ beveragesCount }})</span>
                  </div>
                  <div class="section-items">
                    <button
                      v-for="cat in drinkSubcategories"
                      :key="cat"
                      type="button"
                      class="cat-menu-item"
                      :class="{ selected: itemCategoryFilter === cat }"
                      role="option"
                      :aria-selected="itemCategoryFilter === cat"
                      @click="selectCategoryFilter(cat)"
                    >
                      <div class="item-left">
                        <span class="f-pill cat-preview-pill" :class="getPillClass(cat)">
                          {{ cat }}
                        </span>
                      </div>
                      <div class="item-right">
                        <span class="item-count-badge">{{ getCategoryCount(cat) }}</span>
                        <span v-if="itemCategoryFilter === cat" class="check-mark">✓</span>
                      </div>
                    </button>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
          <span class="result-count"
            >{{ filteredManagerItems.length }} of {{ menuItems.length }} items</span
          >
        </div>

        <div class="table-container">
          <table v-if="filteredManagerItems.length > 0" class="data-table">
            <thead>
              <tr>
                <th>Image</th>
                <th>Name</th>
                <th>Category</th>
                <th>Image File</th>
                <th class="actions-col">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredManagerItems" :key="item.id">
                <td>
                  <div
                    class="table-thumb"
                    role="img"
                    :aria-label="`${item.name} image`"
                    :style="
                      item.imageName ? { backgroundImage: `url('${getImagePath(item)}')` } : {}
                    "
                  ></div>
                </td>
                <td class="fw-bold">{{ item.name }}</td>
                <td>
                  <span
                    class="f-pill"
                    :class="getPillClass(item.category)"
                    style="
                      display: inline-block;
                      padding: 4px 12px;
                      font-size: 0.85rem;
                      pointer-events: none;
                    "
                  >
                    {{ item.category }}
                  </span>
                </td>
                <td class="code-font">{{ item.imageName || 'No image attached' }}</td>
                <td class="actions-col">
                  <button class="action-btn edit-btn" @click="openEditModal(item)">Edit</button>
                  <button class="action-btn del-btn" @click="confirmDeleteItem(item)">
                    Delete
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="manager-empty">
            <div class="empty-mark" aria-hidden="true">⌕</div>
            <h3>No menu items found</h3>
            <p>
              {{
                itemSearch
                  ? `No items match “${itemSearch}”.`
                  : 'Add your first menu item to make it available in the survey.'
              }}
            </p>
            <button v-if="itemSearch" class="action-btn edit-btn" @click="itemSearch = ''">
              Clear search
            </button>
            <button v-else class="nav-btn orange-solid" @click="openNewItemModal">
              Add your first item
            </button>
          </div>
        </div>
      </div>

      <div v-if="activeAdminTab === 'descriptions'" class="manager-layout fade-in">
        <div class="manager-header-row item-manager-header">
          <div>
            <h2>Item Descriptions</h2>
            <p class="manager-description">Write the descriptions shown with each item in the public survey.</p>
          </div>
          <span class="result-count">{{ menuItems.length }} items</span>
        </div>

        <div class="description-editor-layout">
          <aside class="description-item-list" aria-label="Menu items">
            <label class="search-field description-search">
              <span aria-hidden="true">⌕</span>
              <span class="sr-only">Search items</span>
              <input v-model="descriptionSearch" type="search" placeholder="Find an item" />
            </label>
            <button
              v-for="item in filteredDescriptionItems"
              :key="item.id"
              type="button"
              class="description-item-option"
              :class="{ active: descriptionItem?.id === item.id }"
              @click="descriptionItemId = item.id"
            >
              <span class="description-option-name">{{ item.name }}</span>
              <span class="description-option-meta">{{ item.category }}</span>
              <span class="description-status" :class="{ written: item.description?.trim() }">
                {{ item.description?.trim() ? 'Custom description' : 'Using default' }}
              </span>
            </button>
            <p v-if="filteredDescriptionItems.length === 0" class="description-list-empty">
              No items match this search.
            </p>
          </aside>

          <section v-if="descriptionItem" class="description-editor" aria-labelledby="description-editor-title">
            <div class="description-editor-heading">
              <div>
                <span class="f-pill" :class="getPillClass(descriptionItem.category)">{{ descriptionItem.category }}</span>
                <h3 id="description-editor-title">{{ descriptionItem.name }}</h3>
                <p>Custom descriptions replace the default item copy on the survey.</p>
              </div>
              <span class="description-updated">
                {{ descriptionItem.descriptionUpdatedAt ? `Updated ${new Date(descriptionItem.descriptionUpdatedAt).toLocaleString()}` : 'No saved edits yet' }}
              </span>
            </div>

            <form class="description-form" @submit.prevent="saveItemDescription">
              <label for="item-description">Description</label>
              <textarea
                id="item-description"
                v-model="descriptionDraft"
                maxlength="1000"
                rows="7"
                placeholder="Add a short description of the ingredients, flavor, or serving style. Leave blank to use the default description."
              ></textarea>
              <div class="description-form-footer">
                <span>{{ descriptionDraft.length }} / 1000 characters</span>
                <button type="submit" class="nav-btn orange-solid" :disabled="isSavingDescription">
                  {{ isSavingDescription ? 'Saving…' : 'Save description' }}
                </button>
              </div>
            </form>

            <div class="description-preview">
              <span class="description-preview-label">SURVEY PREVIEW</span>
              <p v-if="descriptionDraft.trim()">{{ descriptionDraft.trim() }}</p>
              <span v-else class="description-fallback-note">No description will appear on the survey until you save one.</span>
            </div>
          </section>

          <div v-else class="description-editor-empty">Add a menu item to start writing descriptions.</div>
        </div>
      </div>

      <div v-if="activeAdminTab === 'questions'" class="manager-layout fade-in">
        <div class="manager-header-row question-manager-header">
          <div>
            <h2>Survey Question Manager</h2>
            <p class="manager-description">
              Organized into 2 sections: Respondent Demographics and Menu Item Evaluations.
            </p>
          </div>
          <div class="manager-header-actions">
            <button
              class="nav-btn orange-solid"
              @click="openNewQuestionModal"
              style="white-space: nowrap; padding: 10px 20px"
            >
              + Add New Question
            </button>
          </div>
        </div>

        <div class="table-container question-manager-body" style="padding: 24px">
          <!-- ======================================================== -->
          <!-- SECTION 1 — Respondent Information                       -->
          <!-- ======================================================== -->
          <div class="qm-section-group mb-5">
            <div class="qm-section-header demo-header">
              <div class="qm-header-left">
                <div class="qm-icon-box blue-box">👤</div>
                <div>
                  <div class="scope-label"><span class="scope-dot blue-dot"></span> SECTION 1</div>
                  <h3 class="qm-section-title">SECTION 1 — Respondent Information</h3>
                  <p class="qm-section-desc">
                    Demographic questions answered by respondents before rating menu items.
                  </p>
                </div>
              </div>
              <span class="qm-badge blue-pill">2 Questions • Baseline Demographics</span>
            </div>

            <div class="qm-cards-list">
              <div
                v-for="(q, index) in demographicQuestions"
                :key="q.id"
                class="insight-card mb-3 demo-q-card"
              >
                <div class="manager-header-row q-card-top-row">
                  <div class="q-card-title-group">
                    <span class="q-circle demo-q-num">Q1.{{ index + 1 }}</span>
                    <div>
                      <strong class="q-title-text">{{ q.text }}</strong>
                      <div class="q-badge-row">
                        <span class="badge-v2 demo-scope-badge">👤 Demographic</span>
                        <span class="badge-v2 type-badge">🔘 Multiple choice</span>
                      </div>
                    </div>
                  </div>
                  <span class="fixed-indicator-badge">Fixed Baseline</span>
                </div>

                <div class="q-card-options-row">
                  <p class="section-label mb-2">Available Options ({{ q.options.length }}):</p>
                  <div class="options-pills-row">
                    <span
                      v-for="(opt, oIdx) in q.options"
                      :key="opt"
                      class="f-pill pill-demo"
                    >
                      <span class="opt-num">{{ oIdx + 1 }}.</span>
                      <span>{{ opt }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- ======================================================== -->
          <!-- SECTION 2 — Menu Item Evaluation Questions               -->
          <!-- ======================================================== -->
          <div class="qm-section-group">
            <div class="qm-section-header menu-header">
              <div class="qm-header-left">
                <div class="qm-icon-box orange-box">🍴</div>
                <div>
                  <div class="scope-label"><span class="scope-dot orange-dot"></span> SECTION 2</div>
                  <h3 class="qm-section-title">SECTION 2 — Menu Item Evaluation Questions</h3>
                  <p class="qm-section-desc">
                    Evaluation questions asked for every food and beverage item in the survey.
                  </p>
                </div>
              </div>
              <div class="qm-header-right">
                <span class="qm-badge orange-pill">
                  {{ section2EvaluationQuestions.length + (customQuestions.length > 0 ? customQuestions.length : 0) }} Questions • Live Survey Grid
                </span>
                <button
                  class="nav-btn orange-solid add-q-sub-btn"
                  @click="openNewQuestionModal"
                >
                  + Add Question
                </button>
              </div>
            </div>

            <div class="qm-cards-list">
              <!-- Live Section 2 Questions (Matching Survey.vue) -->
              <div
                v-for="q in section2EvaluationQuestions"
                :key="q.id"
                class="insight-card mb-4 menu-q-card"
              >
                <div class="manager-header-row q-card-top-row">
                  <div class="q-card-title-group">
                    <span class="q-circle menu-q-num">{{ q.numberLabel }}</span>
                    <div>
                      <strong class="q-title-text">{{ q.title }}</strong>
                      <p class="section-subtext mb-1 prompt-text">
                        “{{ q.prompt }}”
                      </p>
                      <div class="q-badge-row">
                        <span class="badge-v2 food-badge">{{ q.scopeLabel }}</span>
                        <span class="badge-v2 type-badge">📊 {{ q.typeLabel }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="q-actions-row">
                    <span class="fixed-indicator-badge">Live in Survey</span>
                    <button
                      type="button"
                      class="action-btn edit-btn"
                      @click="openEditEvaluationQuestionModal(q)"
                      title="Edit question text / prompt"
                    >
                      ✏️ Edit Question
                    </button>
                  </div>
                </div>

                <!-- Rating scale strip -->
                <div class="q-card-scale-row mb-3">
                  <p class="section-label mb-1">Likert Rating Scale (1 to 5):</p>
                  <div class="scale-pills-row">
                    <span
                      v-for="scale in q.scale"
                      :key="scale.value"
                      class="scale-pill"
                    >
                      <strong class="scale-val">{{ scale.value }}</strong>
                      <span class="scale-lbl">{{ scale.label }}</span>
                    </span>
                  </div>
                </div>

                <!-- Evaluation rows -->
                <div class="q-card-options-row">
                  <div style="display: flex; justify-content: space-between; align-items: center;" class="mb-2">
                    <p class="section-label mb-0">
                      Evaluation Dimensions / Rows ({{ q.rows.length }}):
                    </p>
                    <button
                      type="button"
                      class="nav-btn orange-outline add-dim-btn"
                      @click="openAddDimensionModal(q)"
                      style="font-size: 0.8rem; padding: 4px 10px;"
                    >
                      + Add Dimension
                    </button>
                  </div>
                  <div class="options-pills-row">
                    <span
                      v-for="(row, rIdx) in q.rows"
                      :key="row.id"
                      class="f-pill pill-matrix-dim"
                      :id="`dimension-pill-${row.id}`"
                      style="display: inline-flex; align-items: center; gap: 6px;"
                    >
                      <span class="opt-num-orange">{{ rIdx + 1 }}.</span>
                      <span v-if="row.icon">{{ row.icon }}</span>
                      <span>{{ row.label }}</span>
                      <button
                        type="button"
                        class="edit-dim-btn"
                        :id="`edit-dim-btn-${row.id}`"
                        @click.stop="openEditDimensionModal(q, row)"
                        title="Edit dimension"
                        aria-label="Edit dimension"
                      >
                        ✏️
                      </button>
                      <button
                        type="button"
                        class="del-opt-btn"
                        :id="`del-dim-btn-${row.id}`"
                        @click.stop="confirmDeleteDimension(q, row)"
                        title="Delete dimension"
                        aria-label="Delete dimension"
                      >
                        ✕
                      </button>
                    </span>
                    <button
                      type="button"
                      class="f-pill"
                      @click="openAddDimensionModal(q)"
                      style="border: 1px dashed #ea580c; background: rgba(234, 88, 12, 0.05); color: #ea580c; cursor: pointer; font-weight: 600;"
                    >
                      + Add Dimension
                    </button>
                  </div>
                  <p class="grid-req-note mt-2">
                    <span class="req-asterisk">*</span> {{ q.requiredNote }}
                  </p>
                </div>
              </div>

              <!-- Custom / Additional Questions if any -->
              <template v-if="customQuestions.length > 0">
                <div class="custom-questions-divider my-4">
                  <div class="scope-label"><span class="scope-dot orange-dot"></span> ADDITIONAL CUSTOM QUESTIONS</div>
                </div>
                <div
                  v-for="(q, index) in customQuestions"
                  :key="q.id"
                  class="insight-card mb-4 menu-q-card"
                >
                  <div class="manager-header-row q-card-top-row">
                    <div class="q-card-title-group">
                      <span class="q-circle">Q2.{{ section2EvaluationQuestions.length + index + 1 }}</span>
                      <div>
                        <strong class="q-title-text">{{ q.text }}</strong>
                        <div class="q-badge-row">
                          <span class="badge-v2 food-badge">🍴 Item Evaluation</span>
                          <span
                            class="badge-v2"
                            :class="q.type === 'TEXT' ? 'type-badge' : 'food-badge'"
                          >
                            {{ q.type === 'TEXT' ? '💬 Open-ended (Text)' : '🔘 Multiple Choice (Radio)' }}
                          </span>
                        </div>
                      </div>
                    </div>
                    <div class="q-actions-row">
                      <button class="action-btn edit-btn" @click="openEditQuestionModal(q)">
                        ✏️ Edit
                      </button>
                      <button class="action-btn del-btn" @click="confirmDeleteQuestion(q.id, q.text)">🗑️</button>
                    </div>
                  </div>

                  <div v-if="q.type !== 'TEXT'" class="q-card-options-row">
                    <p class="section-label mb-2">Available Options ({{ q.options?.length || 0 }}):</p>
                    <div class="options-pills-row">
                      <span
                        v-for="opt in q.options"
                        :key="opt.id"
                        class="f-pill"
                        style="display: flex; align-items: center; gap: 8px"
                      >
                        <span v-if="opt.icon">{{ opt.icon }}</span>
                        <span>{{ opt.text || opt.label || opt.name || '⚠️ Blank Option' }}</span>
                        <button
                          @click="confirmDeleteOption(opt.id)"
                          class="del-opt-btn"
                          title="Delete option"
                        >
                          ✕
                        </button>
                      </span>
                      <button
                        @click="openAddOptionModal(q.id)"
                        class="f-pill"
                        style="border: 1px dashed #cbd5e1; background: transparent; cursor: pointer"
                      >
                        + Add Option
                      </button>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 5: STATISTICAL INFERENCE (T-TEST & ANOVA)            -->
      <!-- ======================================================== -->
      <div v-show="activeAdminTab === 'statistics'" class="manager-layout fade-in stats-page-layout">
        <!-- Header Row -->
        <div class="manager-header-row stats-header-row">
          <div>
            <div class="stats-badge-pill">
              <span class="live-dot-teal"></span> INFERENTIAL STATISTICS ENGINE
            </div>
            <h2>Hypothesis Testing & Significance Analysis</h2>
            <p class="manager-subtitle">
              Evaluate whether differences across menu items, categories, moods, weather, and customer demographics are statistically significant using Welch's t-test and Fisher's One-Way ANOVA.
            </p>
          </div>
          <div class="stats-header-kpis" v-if="statisticalOverview">
            <div class="stat-mini-pill">
              <span class="pill-number">{{ statisticalOverview.totalRatingsCount }}</span>
              <span class="pill-label">Rating Records</span>
            </div>
            <div class="stat-mini-pill">
              <span class="pill-number">{{ statisticalOverview.totalEvaluatorsCount }}</span>
              <span class="pill-label">Evaluators</span>
            </div>
            <button class="nav-btn secondary" @click="fetchStatisticalOverview" :disabled="statsLoading">
              🔄 Refresh
            </button>
          </div>
        </div>

        <!-- Mode Toggle Bar: T-Test vs ANOVA -->
        <div class="stats-method-toggle-card">
          <div class="method-btn-group">
            <button
              class="method-toggle-btn"
              :class="{ active: activeTestType === 'ttest' }"
              @click="activeTestType = 'ttest'"
            >
              <span class="toggle-icon">📊</span>
              <div class="toggle-text-block">
                <strong>Two-Sample T-Test</strong>
                <small>Compare means between 2 groups (Welch's Heteroscedastic t-test)</small>
              </div>
            </button>
            <button
              class="method-toggle-btn"
              :class="{ active: activeTestType === 'anova' }"
              @click="activeTestType = 'anova'"
            >
              <span class="toggle-icon">📈</span>
              <div class="toggle-text-block">
                <strong>One-Way ANOVA</strong>
                <small>Test variance across 3+ groups (Fisher's F-ratio & Eta-Squared)</small>
              </div>
            </button>
          </div>
        </div>

        <!-- Loading indicator -->
        <div v-if="statsLoading" class="state-message">
          <h2>Computing statistical parameters...</h2>
        </div>

        <!-- T-Test Panel -->
        <div v-else-if="activeTestType === 'ttest'" class="stats-test-container">
          <!-- Configuration Card -->
          <div class="stats-config-card">
            <div class="config-header">
              <h3>⚙️ T-Test Configuration</h3>
              <span class="config-sub">Select comparison factor, groups, and significance threshold</span>
            </div>
            <div class="config-grid">
              <div class="config-field">
                <label>Comparison Mode</label>
                <select v-model="tTestMode" class="form-select" @change="onTTestModeChange">
                  <option value="SUPER_CATEGORIES">Meals vs. Beverages (Supercategories)</option>
                  <option value="SUBCATEGORIES">Compare Two Subcategories</option>
                  <option value="ITEMS">Compare Two Menu Items</option>
                  <option value="AGE_GROUPS">Compare Two Age Groups</option>
                  <option value="DINING_FREQUENCY">Compare Two Dining Frequencies</option>
                  <option value="DIMENSIONS">Compare Two Evaluation Dimensions</option>
                </select>
              </div>

              <!-- Group 1 -->
              <div class="config-field" v-if="tTestMode !== 'SUPER_CATEGORIES'">
                <label>Group 1</label>
                <select v-model="tTestGroup1" class="form-select">
                  <option v-for="opt in tTestGroup1Options" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <!-- Group 2 -->
              <div class="config-field" v-if="tTestMode !== 'SUPER_CATEGORIES'">
                <label>Group 2</label>
                <select v-model="tTestGroup2" class="form-select">
                  <option v-for="opt in tTestGroup2Options" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <!-- Dimension Filter -->
              <div class="config-field" v-if="tTestMode !== 'DIMENSIONS'">
                <label>Evaluation Dimension</label>
                <select v-model="tTestDimension" class="form-select">
                  <option value="all">All (Overall Suitability)</option>
                  <option v-for="dim in availableDimensionOptions" :key="dim.key" :value="dim.key">
                    {{ dim.icon ? `${dim.icon} ` : '' }}{{ dim.label }}
                  </option>
                </select>
              </div>

              <!-- Alpha Level -->
              <div class="config-field">
                <label>Significance Level (α)</label>
                <select v-model.number="tTestAlpha" class="form-select">
                  <option :value="0.05">α = 0.05 (95% Confidence)</option>
                  <option :value="0.01">α = 0.01 (99% Confidence)</option>
                  <option :value="0.10">α = 0.10 (90% Confidence)</option>
                </select>
              </div>

              <div class="config-action">
                <button class="nav-btn primary run-test-btn" @click="executeTTest" :disabled="statsLoading">
                  🚀 Calculate T-Test
                </button>
              </div>
            </div>
          </div>

          <!-- T-Test Results Display -->
          <div v-if="tTestResult" class="stats-result-wrapper">
            <!-- Hypothesis & Outcome Card -->
            <div class="hypothesis-summary-card" :class="tTestResult.significant ? 'is-sig' : 'not-sig'">
              <div class="hypo-top">
                <div class="hypo-info">
                  <span class="badge-tag">{{ tTestResult.testName }}</span>
                  <p class="hypo-formula"><strong>Hypothesis:</strong> {{ tTestResult.hypothesis }}</p>
                </div>
                <div class="sig-badge" :class="tTestResult.significant ? 'badge-sig' : 'badge-nonsig'">
                  <span class="dot-indicator"></span>
                  {{ tTestResult.significant ? 'Statistically Significant (p < α)' : 'No Significant Difference (p ≥ α)' }}
                </div>
              </div>
              <!-- Executive Summary Callout -->
              <div class="takeaway-box">
                <div class="takeaway-icon">💡</div>
                <div class="takeaway-text">
                  <h4>Managerial Conclusion & Business Insight</h4>
                  <p>{{ tTestResult.conclusion }}</p>
                </div>
              </div>
            </div>

            <!-- Group Comparison Cards -->
            <div class="stats-comparison-row">
              <div class="group-stat-card card-g1">
                <div class="card-g-tag">Group 1</div>
                <h3>{{ tTestResult.group1Label }}</h3>
                <div class="g-score-hero">
                  <span class="hero-score">{{ tTestResult.mean1.toFixed(2) }}</span>
                  <span class="hero-scale">/ 5.0</span>
                </div>
                <div class="g-metrics-grid">
                  <div><strong>Ratings (N):</strong> {{ tTestResult.n1 }}</div>
                  <div><strong>SD (s):</strong> {{ tTestResult.sd1.toFixed(2) }}</div>
                </div>
              </div>

              <div class="vs-divider-card">
                <div class="diff-badge">
                  <span>Difference (Δ):</span>
                  <strong>{{ tTestResult.meanDifference > 0 ? '+' : '' }}{{ tTestResult.meanDifference.toFixed(2) }}</strong>
                </div>
                <div class="ci-text">
                  95% CI: [{{ tTestResult.ciLower.toFixed(2) }}, {{ tTestResult.ciUpper.toFixed(2) }}]
                </div>
              </div>

              <div class="group-stat-card card-g2">
                <div class="card-g-tag">Group 2</div>
                <h3>{{ tTestResult.group2Label }}</h3>
                <div class="g-score-hero">
                  <span class="hero-score">{{ tTestResult.mean2.toFixed(2) }}</span>
                  <span class="hero-scale">/ 5.0</span>
                </div>
                <div class="g-metrics-grid">
                  <div><strong>Ratings (N):</strong> {{ tTestResult.n2 }}</div>
                  <div><strong>SD (s):</strong> {{ tTestResult.sd2.toFixed(2) }}</div>
                </div>
              </div>
            </div>

            <!-- Detailed Statistical Table -->
            <div class="stats-table-card">
              <div class="table-card-header">
                <h4>📐 Test Statistics & Effect Size</h4>
              </div>
              <div class="table-card-body">
                <table class="stats-report-table">
                  <thead>
                    <tr>
                      <th>t-Statistic</th>
                      <th>Degrees of Freedom (df)</th>
                      <th>Standard Error (SE)</th>
                      <th>p-Value (2-tailed)</th>
                      <th>Cohen's d</th>
                      <th>Effect Magnitude</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="font-mono font-bold">{{ tTestResult.tStatistic.toFixed(3) }}</td>
                      <td class="font-mono">{{ tTestResult.degreesOfFreedom.toFixed(1) }}</td>
                      <td class="font-mono">{{ tTestResult.standardError.toFixed(3) }}</td>
                      <td class="font-mono font-bold" :class="tTestResult.significant ? 'text-teal' : ''">
                        {{ tTestResult.pValue < 0.001 ? '< 0.001' : tTestResult.pValue.toFixed(4) }}
                      </td>
                      <td class="font-mono font-bold">{{ tTestResult.cohensD.toFixed(2) }}</td>
                      <td>
                        <span class="effect-badge">{{ tTestResult.effectSizeLabel }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- ANOVA Panel -->
        <div v-else class="stats-test-container">
          <!-- Configuration Card -->
          <div class="stats-config-card">
            <div class="config-header">
              <h3>⚙️ One-Way ANOVA Configuration</h3>
              <span class="config-sub">Analyze variance across 3 or more groups</span>
            </div>
            <div class="config-grid">
              <div class="config-field">
                <label>Grouping Factor</label>
                <select v-model="anovaFactor" class="form-select">
                  <option value="SUBCATEGORIES">Menu Subcategories (Pasta, Waffle, Coffee, etc.)</option>
                  <option value="SUPER_CATEGORIES">Supercategories (Meals vs. Beverages)</option>
                  <option value="AGE_GROUPS">Age Demographic Groups</option>
                  <option value="DINING_FREQUENCY">Dining Frequency Segments</option>
                  <template v-if="statisticalOverview?.evaluationQuestions?.length">
                    <option
                      v-for="eq in statisticalOverview.evaluationQuestions"
                      :key="eq.factorKey"
                      :value="eq.factorKey"
                    >
                      {{ eq.factorLabel }}
                    </option>
                  </template>
                  <template v-else>
                    <option value="MOOD_DIMENSIONS">Mood Dimensions (from Database)</option>
                    <option value="WEATHER_DIMENSIONS">Weather Conditions (from Database)</option>
                  </template>
                  <option value="ALL_DIMENSIONS">All Evaluation Dimensions Combined</option>
                </select>
              </div>

              <!-- Item Filter (for Mood/Weather/Questions) -->
              <div class="config-field" v-if="isDimensionAnovaFactor">
                <label>Filter to Specific Item (Optional)</label>
                <select v-model="anovaItemId" class="form-select">
                  <option :value="null">All Menu Items Combined</option>
                  <option v-for="it in statisticalOverview?.items" :key="it.id" :value="it.id">
                    {{ it.name }} ({{ it.ratingCount }} ratings)
                  </option>
                </select>
              </div>

              <!-- Dimension Filter (for Subcategories & Demographics) -->
              <div class="config-field" v-if="!isDimensionAnovaFactor">
                <label>Evaluation Dimension</label>
                <select v-model="anovaDimension" class="form-select">
                  <option value="all">All (Overall Suitability)</option>
                  <option v-for="dim in availableDimensionOptions" :key="dim.key" :value="dim.key">
                    {{ dim.icon ? `${dim.icon} ` : '' }}{{ dim.label }}
                  </option>
                </select>
              </div>

              <!-- Alpha Level -->
              <div class="config-field">
                <label>Significance Level (α)</label>
                <select v-model.number="anovaAlpha" class="form-select">
                  <option :value="0.05">α = 0.05 (95% Confidence)</option>
                  <option :value="0.01">α = 0.01 (99% Confidence)</option>
                  <option :value="0.10">α = 0.10 (90% Confidence)</option>
                </select>
              </div>

              <div class="config-action">
                <button class="nav-btn primary run-test-btn" @click="executeAnova" :disabled="statsLoading">
                  🚀 Calculate ANOVA
                </button>
              </div>
            </div>
          </div>

          <!-- ANOVA Results Display -->
          <div v-if="anovaResult" class="stats-result-wrapper">
            <!-- Hypothesis & Outcome Card -->
            <div class="hypothesis-summary-card" :class="anovaResult.significant ? 'is-sig' : 'not-sig'">
              <div class="hypo-top">
                <div class="hypo-info">
                  <span class="badge-tag">{{ anovaResult.testName }}</span>
                  <p class="hypo-formula"><strong>Hypothesis:</strong> {{ anovaResult.hypothesis }}</p>
                </div>
                <div class="sig-badge" :class="anovaResult.significant ? 'badge-sig' : 'badge-nonsig'">
                  <span class="dot-indicator"></span>
                  {{ anovaResult.significant ? 'Statistically Significant (p < α)' : 'No Significant Difference (p ≥ α)' }}
                </div>
              </div>
              <!-- Executive Summary Callout -->
              <div class="takeaway-box">
                <div class="takeaway-icon">💡</div>
                <div class="takeaway-text">
                  <h4>Managerial Conclusion & Business Insight</h4>
                  <p>{{ anovaResult.conclusion }}</p>
                </div>
              </div>
            </div>

            <!-- Group Breakdown Cards & Bars -->
            <div class="stats-table-card">
              <div class="table-card-header">
                <div>
                  <h4>📊 Group Means, Sample Sizes & Confidence Intervals</h4>
                  <p class="sample-size-explainer">
                    ℹ️ <strong>Sample Size (N) Explained:</strong> Represents total rating observations across evaluated items (each participant rates all matrix dimensions per item). Filtering by a specific dimension restricts N to that dimension alone.
                  </p>
                </div>
                <span class="grand-mean-tag">Grand Mean: <strong>{{ anovaResult.grandMean.toFixed(2) }}</strong> (Total N = {{ anovaResult.totalN }} ratings)</span>
              </div>
              <div class="table-card-body">
                <table class="stats-report-table">
                  <thead>
                    <tr>
                      <th>Group Name</th>
                      <th>Observations (N ratings)</th>
                      <th>Mean Score (1–5)</th>
                      <th>Std Dev (s)</th>
                      <th>Std Error (SE)</th>
                      <th>95% Confidence Interval</th>
                      <th>Score Distribution</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="grp in anovaResult.groups" :key="grp.groupName">
                      <td class="font-bold">{{ grp.groupName }}</td>
                      <td class="font-mono">{{ grp.n }}</td>
                      <td class="font-mono font-bold">{{ grp.mean.toFixed(2) }}</td>
                      <td class="font-mono">{{ grp.stdDev.toFixed(2) }}</td>
                      <td class="font-mono">{{ grp.standardError.toFixed(3) }}</td>
                      <td class="font-mono">[{{ grp.ciLower.toFixed(2) }}, {{ grp.ciUpper.toFixed(2) }}]</td>
                      <td>
                        <div class="mini-score-bar-track">
                          <div class="mini-score-fill" :style="{ width: ((grp.mean / 5.0) * 100) + '%' }"></div>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Standard ANOVA Summary Table -->
            <div class="stats-table-card">
              <div class="table-card-header">
                <h4>📐 ANOVA Summary Source Table (Between vs. Within Groups)</h4>
                <div class="effect-header-badge">
                  <span>Variance Explained (η²):</span>
                  <strong>{{ (anovaResult.etaSquared * 100).toFixed(1) }}%</strong>
                  <span class="effect-sub-badge">({{ anovaResult.effectSizeLabel }})</span>
                </div>
              </div>
              <div class="table-card-body">
                <table class="stats-report-table">
                  <thead>
                    <tr>
                      <th>Source of Variation</th>
                      <th>Sum of Squares (SS)</th>
                      <th>df</th>
                      <th>Mean Square (MS)</th>
                      <th>F-Ratio</th>
                      <th>p-Value</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="font-bold">Between Groups (Factor)</td>
                      <td class="font-mono">{{ anovaResult.ssBetween.toFixed(3) }}</td>
                      <td class="font-mono">{{ anovaResult.dfBetween }}</td>
                      <td class="font-mono font-bold">{{ anovaResult.msBetween.toFixed(3) }}</td>
                      <td class="font-mono font-bold text-teal" rowspan="2" style="vertical-align: middle">
                        {{ anovaResult.fStatistic.toFixed(3) }}
                      </td>
                      <td class="font-mono font-bold text-teal" rowspan="2" style="vertical-align: middle">
                        {{ anovaResult.pValue < 0.001 ? '< 0.001' : anovaResult.pValue.toFixed(4) }}
                      </td>
                    </tr>
                    <tr>
                      <td class="font-bold">Within Groups (Error)</td>
                      <td class="font-mono">{{ anovaResult.ssWithin.toFixed(3) }}</td>
                      <td class="font-mono">{{ anovaResult.dfWithin }}</td>
                      <td class="font-mono font-bold">{{ anovaResult.msWithin.toFixed(3) }}</td>
                    </tr>
                    <tr class="total-row">
                      <td class="font-bold">Total</td>
                      <td class="font-mono">{{ anovaResult.ssTotal.toFixed(3) }}</td>
                      <td class="font-mono">{{ anovaResult.dfTotal }}</td>
                      <td>—</td>
                      <td>—</td>
                      <td>—</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Teleport to="body">
        <div v-if="showClearModal" class="modal-overlay">
          <div class="modal-card danger-card">
            <div class="modal-icon text-red">⚠️</div>
            <h2>Wipe All Data?</h2>
            <p>
              This will permanently delete <strong>all survey responses</strong> from the database.
              This action cannot be undone!
            </p>
            <div class="modal-actions">
              <button
                type="button"
                class="nav-btn secondary"
                :disabled="isClearingData"
                @click="showClearModal = false"
              >
                Cancel
              </button>
              <button
                type="button"
                class="nav-btn danger-solid"
                :disabled="isClearingData"
                @click="clearAllData"
              >
                {{ isClearingData ? 'Clearing…' : 'Yes, Nuke It' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showItemModal" class="modal-overlay" @click.self="closeItemModal">
          <div
            class="modal-card form-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="item-modal-title"
          >
            <div class="modal-heading-row">
              <div>
                <span class="modal-kicker">{{ editingItem.id ? 'EDIT ITEM' : 'NEW ITEM' }}</span>
                <h2 id="item-modal-title">
                  {{ editingItem.id ? 'Edit Menu Item' : 'Create New Item' }}
                </h2>
              </div>
              <button
                type="button"
                class="modal-close"
                aria-label="Close item editor"
                @click="closeItemModal"
              >
                ×
              </button>
            </div>
            <p class="form-intro">Updates will immediately reflect on the live survey.</p>

            <form @submit.prevent="saveMenuItem" class="edit-form">
              <div class="form-group">
                <label for="menu-item-name"
                  >Item Name <span class="required-mark">Required</span></label
                >
                <input
                  id="menu-item-name"
                  type="text"
                  v-model="editingItem.name"
                  required
                  placeholder="e.g. Classic Waffle"
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <div class="form-label-row">
                  <label for="menu-item-category"
                    >Category <span class="required-mark">Required</span></label
                  >
                  <span
                    v-if="editingItem.category"
                    class="f-pill form-pill-preview"
                    :class="getPillClass(editingItem.category)"
                  >
                    {{ editingItem.category }}
                  </span>
                </div>
                <div class="custom-select-wrapper">
                  <select
                    id="menu-item-category"
                    v-model="editingItem.category"
                    required
                    class="form-input custom-styled-select"
                  >
                    <optgroup label="🍽️ Meals">
                      <option value="APPETIZER">APPETIZER</option>
                      <option value="PASTA">PASTA</option>
                      <option value="SANDWICH & WRAPS">SANDWICH & WRAPS</option>
                      <option value="CHICKEN WINGS">CHICKEN WINGS</option>
                      <option value="RICE MEAL">RICE MEAL</option>
                    </optgroup>
                    <optgroup label="🥤 Beverages">
                      <option value="CLASSICS">CLASSICS</option>
                      <option value="ICE-BLENDED">ICE-BLENDED</option>
                      <option value="SPECIALTY">SPECIALTY</option>
                      <option value="NON-COFFEE">NON-COFFEE</option>
                      <option value="REFRESHER">REFRESHER</option>
                      <option value="CEREMONIAL MATCHA">CEREMONIAL MATCHA</option>
                    </optgroup>
                  </select>
                  <span class="custom-select-arrow" aria-hidden="true">
                    <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
                      <path
                        fill-rule="evenodd"
                        d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                        clip-rule="evenodd"
                      />
                    </svg>
                  </span>
                </div>
              </div>

              <div class="form-group">
                <label>Item image <span class="optional-mark">Optional</span></label>
                <input
                  ref="imageFileInput"
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/jpg"
                  class="sr-only"
                  tabindex="-1"
                  @change="onImageFileChange"
                />
                <div class="image-upload-box">
                  <div
                    v-if="imagePreviewUrl"
                    class="preview-thumb"
                    :style="{ backgroundImage: `url('${imagePreviewUrl}')` }"
                  ></div>
                  <div v-else class="preview-empty">🖼️</div>
                  <div class="upload-controls">
                    <p class="code-font mb-2">
                      {{ editingItem.imageName ? editingItem.imageName : 'No image selected' }}
                    </p>
                    <p v-if="imageError" class="error-text" style="margin: 0 0 8px 0">
                      {{ imageError }}
                    </p>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap">
                      <button
                        type="button"
                        class="f-btn pos-btn upload-btn"
                        :disabled="isUploadingImage"
                        @click="triggerImagePicker"
                      >
                        {{ isUploadingImage ? 'Uploading...' : 'Choose from device' }}
                      </button>
                      <button
                        v-if="editingItem.imageName || imagePreviewUrl"
                        type="button"
                        class="f-btn"
                        :disabled="isUploadingImage"
                        @click="clearImage"
                      >
                        Remove
                      </button>
                    </div>
                    <p class="section-subtext" style="margin: 8px 0 0 0">
                      PNG, JPG or WEBP up to 5MB
                    </p>
                  </div>
                </div>
              </div>

              <div class="modal-actions mt-4">
                <button type="button" class="nav-btn secondary" @click="closeItemModal">
                  Cancel
                </button>
                <button
                  type="submit"
                  class="nav-btn orange-solid"
                  :disabled="isSavingItem || isUploadingImage"
                >
                  {{ isSavingItem ? 'Saving...' : isUploadingImage ? 'Uploading image...' : '💾 Save Item' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showQuestionModal" class="modal-overlay">
          <div class="modal-card form-card">
            <h2>{{ editingQuestion.id ? 'Edit Question' : 'Create New Question' }}</h2>
            <p class="section-subtext mb-4">
              Note: Changing a question's text will update it on the live survey immediately.
            </p>

            <form @submit.prevent="saveQuestion" class="edit-form">
              <div class="form-group">
                <label>Question Text</label>
                <input
                  type="text"
                  v-model="editingQuestion.text"
                  required
                  placeholder="e.g. How was the presentation?"
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <label>Input Type</label>
                <select v-model="editingQuestion.type" required class="form-input">
                  <option value="RADIO">🔘 Multiple Choice (Radio Buttons)</option>
                  <option value="TEXT">💬 Open-ended (Text Area)</option>
                  <option value="MATRIX">📊 Rating Matrix (1–5 Likert Scale)</option>
                </select>
              </div>

              <div class="modal-actions mt-4">
                <button type="button" class="nav-btn secondary" @click="showQuestionModal = false">
                  Cancel
                </button>
                <button type="submit" class="nav-btn orange-solid" :disabled="isSavingQuestion">
                  {{ isSavingQuestion ? 'Saving...' : '💾 Save Question' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showLogoutModal" class="modal-overlay">
          <div class="modal-card form-card" style="text-align: center; max-width: 400px">
            <div class="modal-icon" style="font-size: 3rem; margin-bottom: 15px">👋</div>
            <h2 style="color: #0f172a">Ready to leave?</h2>
            <p class="section-subtext mb-4">
              Are you sure you want to log out of the admin dashboard?
            </p>

            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" @click="showLogoutModal = false">Cancel</button>

              <button class="nav-btn orange-solid" @click="handleLogout">Yes, Log Out</button>
            </div>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showAddOptionModal" class="modal-overlay">
          <div class="modal-card form-card">
            <h2>Add New Option</h2>
            <p class="section-subtext mb-4">
              Create a new choice for this multiple-choice question.
            </p>

            <form @submit.prevent="submitNewOption" class="edit-form">
              <div class="form-group">
                <label>Option Text / Label</label>
                <input
                  type="text"
                  v-model="optionForm.label"
                  required
                  placeholder="e.g. Too Salty, Perfect, etc."
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <label>Emoji Icon (Optional)</label>
                <div class="emoji-picker-container">
                  <div class="selected-emoji-preview">
                    {{ optionForm.icon || '❌' }}
                  </div>
                  <div class="emoji-grid">
                    <button
                      type="button"
                      v-for="emo in quickEmojis"
                      :key="emo"
                      @click="optionForm.icon = emo"
                      class="emo-btn"
                      :class="{ 'active-emo': optionForm.icon === emo }"
                    >
                      {{ emo }}
                    </button>
                    <button
                      type="button"
                      @click="optionForm.icon = ''"
                      class="emo-btn text-red"
                      title="Clear Emoji"
                    >
                      🚫
                    </button>
                  </div>
                </div>
              </div>

              <div class="modal-actions mt-4">
                <button type="button" class="nav-btn secondary" @click="showAddOptionModal = false">
                  Cancel
                </button>
                <button type="submit" class="nav-btn orange-solid" :disabled="isSavingOption">
                  {{ isSavingOption ? 'Saving...' : '💾 Add Option' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showDimensionModal" class="modal-overlay">
          <div class="modal-card form-card">
            <h2>{{ dimensionForm.id ? 'Edit Evaluation Dimension' : 'Add Evaluation Dimension' }}</h2>
            <p class="section-subtext mb-4">
              {{ dimensionForm.id ? 'Update this dimension/row for the evaluation question.' : 'Add a new dimension/row for respondents to rate in the survey matrix.' }}
            </p>

            <form @submit.prevent="saveDimension" class="edit-form">
              <div class="form-group">
                <label>Dimension Name / Label *</label>
                <input
                  type="text"
                  v-model="dimensionForm.label"
                  required
                  placeholder="e.g. Energy, Comfort, Refreshing, Late Night"
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <label>Description / Subtitle (Optional)</label>
                <input
                  type="text"
                  v-model="dimensionForm.sub"
                  placeholder="e.g. (Wants something energizing)"
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <label>Emoji Icon (Optional)</label>
                <div class="emoji-picker-container">
                  <div class="selected-emoji-preview">
                    {{ dimensionForm.icon || '—' }}
                  </div>
                  <div class="emoji-grid">
                    <button
                      type="button"
                      v-for="emo in quickEmojis"
                      :key="emo"
                      @click="dimensionForm.icon = emo"
                      class="emo-btn"
                      :class="{ 'active-emo': dimensionForm.icon === emo }"
                    >
                      {{ emo }}
                    </button>
                    <button
                      type="button"
                      @click="dimensionForm.icon = ''"
                      class="emo-btn text-red"
                      title="Clear Emoji"
                    >
                      🚫
                    </button>
                  </div>
                </div>
              </div>

              <div class="modal-actions mt-4" style="display: flex; justify-content: space-between; align-items: center;">
                <button
                  v-if="dimensionForm.id || dimensionForm.rawRow"
                  type="button"
                  id="btn-delete-dim-from-edit-modal"
                  class="nav-btn danger-outline"
                  @click="deleteDimensionFromModal"
                  style="margin-right: auto;"
                >
                  🗑️ Delete
                </button>
                <div style="display: flex; gap: 10px; margin-left: auto;">
                  <button type="button" class="nav-btn secondary" @click="showDimensionModal = false">
                    Cancel
                  </button>
                  <button type="submit" class="nav-btn orange-solid" :disabled="isSavingDimension">
                    {{ isSavingDimension ? 'Saving...' : '💾 Save Dimension' }}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showDeleteDimensionModal" class="modal-overlay">
          <div class="modal-card danger-card" style="text-align: center; max-width: 400px">
            <div class="modal-icon text-red" style="font-size: 3rem; margin-bottom: 15px">🚨</div>
            <h2 style="color: #0f172a">Delete Dimension?</h2>
            <p class="section-subtext mb-2" style="font-style: italic; color: #64748b">
              "{{ dimensionToDelete?.label }}"
            </p>
            <p class="section-subtext mb-4">
              Are you sure you want to delete this dimension? It will no longer appear in the survey rating matrix.
            </p>

            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" :disabled="isDeletingDimension" @click="showDeleteDimensionModal = false">
                Cancel
              </button>
              <button
                id="btn-confirm-delete-dimension"
                class="nav-btn danger-solid"
                :disabled="isDeletingDimension"
                @click="executeDeleteDimension"
              >
                {{ isDeletingDimension ? 'Deleting...' : 'Yes, Delete' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div v-if="showDeleteQuestionModal" class="modal-overlay">
          <div class="modal-card danger-card" style="text-align: center; max-width: 400px">
            <div class="modal-icon text-red" style="font-size: 3rem; margin-bottom: 15px">🚨</div>
            <h2 style="color: #0f172a">Delete Question?</h2>
            <p class="section-subtext mb-2" style="font-style: italic; color: #64748b">
              "{{ questionToDeleteText }}"
            </p>
            <p class="section-subtext mb-4">
              Are you sure? All survey analytics tied to this question will be permanently lost!
            </p>

            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" @click="showDeleteQuestionModal = false">
                Cancel
              </button>
              <button class="nav-btn danger-solid" @click="executeDeleteQuestion">
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div
          v-if="showDeleteItemModal"
          class="modal-overlay"
          @click.self="showDeleteItemModal = false"
        >
          <div
            class="modal-card danger-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-item-title"
            style="text-align: center; max-width: 400px"
          >
            <div class="modal-icon text-red" style="font-size: 3rem; margin-bottom: 15px">🚨</div>
            <h2 id="delete-item-title" style="color: #0f172a">
              Delete “{{ itemPendingDelete?.name }}”?
            </h2>
            <p class="section-subtext mb-4">
              This removes the item from the live survey. Existing responses are kept, but will no
              longer be linked to this menu item.
            </p>

            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" @click="showDeleteItemModal = false">Cancel</button>
              <button
                class="nav-btn danger-solid"
                :disabled="isDeletingItem"
                @click="executeDeleteItem"
              >
                {{ isDeletingItem ? 'Deleting...' : 'Delete item' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <Teleport to="body">
        <div
          v-if="showDeleteAllModal"
          class="modal-overlay"
          @click.self="showDeleteAllModal = false"
        >
          <div
            class="modal-card danger-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-all-title"
            style="text-align: center; max-width: 460px"
          >
            <div class="modal-icon text-red" style="font-size: 3rem; margin-bottom: 15px">🚨</div>
            <h2 id="delete-all-title" style="color: #0f172a">Delete all menu items?</h2>
            <p class="section-subtext mb-4">
              This will permanently remove <strong>{{ menuItems.length }} item{{ menuItems.length === 1 ? '' : 's' }}</strong>
              from the menu manager and the live survey. Existing responses are kept but will no longer be linked to these items. This cannot be undone.
            </p>
            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" @click="showDeleteAllModal = false">Cancel</button>
              <button
                class="nav-btn danger-solid"
                :disabled="isDeletingAll"
                @click="executeDeleteAllItems"
              >
                {{ isDeletingAll ? 'Deleting...' : `Delete all (${menuItems.length})` }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <div v-if="toastMessage" class="admin-toast" :class="`toast-${toastType}`" role="status">
        <span aria-hidden="true">{{ toastType === 'success' ? '✓' : '!' }}</span
        >{{ toastMessage }}
      </div>

      <Teleport to="body">
        <div v-if="showDeleteOptionModal" class="modal-overlay">
          <div class="modal-card danger-card" style="text-align: center; max-width: 400px">
            <div class="modal-icon text-red" style="font-size: 3rem; margin-bottom: 15px">🚨</div>
            <h2 style="color: #0f172a">Remove Option?</h2>
            <p class="section-subtext mb-4">
              Are you sure you want to remove this option? It will disappear from the live survey
              immediately.
            </p>

            <div class="modal-actions" style="justify-content: center; margin-top: 25px">
              <button class="nav-btn secondary" @click="showDeleteOptionModal = false">
                Cancel
              </button>
              <button class="nav-btn danger-solid" @click="executeDeleteOption">
                Yes, Remove It
              </button>
            </div>
          </div>
        </div>
      </Teleport>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import axios from 'axios'
import {
  AGE_GROUP_OPTIONS,
  DINING_FREQUENCY_OPTIONS,
  DRINK_SUBCATEGORIES,
  FOOD_SUBCATEGORIES,
  QUICK_EMOJIS,
  REPORT_FILENAME,
  SECTION_1_DEMOGRAPHIC_QUESTIONS,
  SECTION_2_EVALUATION_QUESTIONS,
  SECTION_2_MOOD_ROWS,
  SECTION_2_WEATHER_ROWS,
  SURVEY_BASELINE_TARGET,
  SURVEY_ITEM_LIMIT,
} from '../config/constants'
import {
  getCategoryPillClass,
  getCategoryStyles,
  getImagePath,
  getItemDescription,
  isDrinkCategory,
  isFoodCategory,
} from '../utils/menu'

export interface GridRowStat {
  id: string
  label: string
  shortLabel: string
  avgRating: number
  totalVotes: number
  suitabilityPct: number
  distribution: Record<number, number>
}

export interface GridQuestionAnalytics {
  title: string
  prompt: string
  rows: GridRowStat[]
  topRowLabel: string | null
  topRowScore: number
  totalEvaluators: number
}

export interface DemographicAnalytics {
  globalParticipants: number
  participantLimit?: number
  totalParticipants: number
  ageGroupCounts: Record<string, number>
  diningFrequencyCounts: Record<string, number>
}

export interface SurveyResponseDetail {
  userId: string
  moodRatings: Record<string, number>
  weatherRatings: Record<string, number>
  textFeedback?: string | null
}

// Restore the admin session before the first render so a page refresh keeps the
// dashboard open and its initial API requests include the saved JWT.
const getStoredAdminToken = () => {
  if (typeof window === 'undefined') return null

  try {
    return window.localStorage.getItem('admin_token')
  } catch {
    return null
  }
}

const storedAdminToken = getStoredAdminToken()
const isAuthenticated = ref(false)
if (storedAdminToken) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${storedAdminToken}`
}

// Security State
const username = ref('')
const password = ref('')
const loginError = ref('')
const isLoggingIn = ref(false)

const handleLogin = async () => {
  isLoggingIn.value = true
  loginError.value = ''

  let token: string
  try {
    const response = await axios.post('/api/admin/login', {
      username: username.value,
      password: password.value,
    })

    token = response.data?.token
    if (typeof token !== 'string' || !token) {
      throw new Error('Admin login did not return a token')
    }
  } catch (error: any) {
    loginError.value = error?.response?.status === 401 || error?.response?.status === 403
      ? 'Invalid username or password.'
      : 'Could not reach the admin server. Please try again.'
    return
  } finally {
    isLoggingIn.value = false
  }

  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  try {
    window.localStorage.setItem('admin_token', token)
  } catch {
    // Keep this session usable when browser storage is unavailable.
  }
  isAuthenticated.value = true
  password.value = ''
  await Promise.all([fetchMenuItems(), fetchQuestions(), fetchStats()])
}

const handleLogout = async () => {
  showLogoutModal.value = false

  delete axios.defaults.headers.common['Authorization']
  try {
    window.localStorage.removeItem('admin_token')
  } catch {
    // The in-memory session is still cleared when browser storage is unavailable.
  }
  isAuthenticated.value = false

  window.location.reload()
}

// Master State
const menuItems = ref<any[]>([])
const analyticsData = ref<Record<string, any>>({})
const isLoading = ref(true)
const menuLoadError = ref(false)
const analyticsLoadError = ref(false)
let analyticsRequestGeneration = 0

const activeCategory = ref('Meals')
const activeSubcategory = ref('All')
const selectedItemId = ref<number | null>(null)

const baselineCount = ref(0)
const showClearModal = ref(false)
const isClearingData = ref(false)

const moodAnalytics = ref<GridQuestionAnalytics | null>(null)
const weatherAnalytics = ref<GridQuestionAnalytics | null>(null)
const demographics = ref<DemographicAnalytics>({
  globalParticipants: 0,
  totalParticipants: 0,
  ageGroupCounts: {},
  diningFrequencyCounts: {},
})
const recentResponses = ref<SurveyResponseDetail[]>([])
const topMood = ref<string | null>(null)
const topMoodScore = ref<number | null>(null)
const topWeather = ref<string | null>(null)
const topWeatherScore = ref<number | null>(null)
const avgSuitabilityScore = ref<number | null>(null)

const foodSubcategories = FOOD_SUBCATEGORIES
const drinkSubcategories = DRINK_SUBCATEGORIES
const isFood = isFoodCategory
const isDrink = isDrinkCategory
const getPillClass = getCategoryPillClass

const currentSubcategories = computed(() => {
  return activeCategory.value === 'Meals' ? foodSubcategories : drinkSubcategories
})

const allSubcategories = [...foodSubcategories, ...drinkSubcategories]
const itemSearch = ref('')
const itemCategoryFilter = ref('All')
const isCategoryDropdownOpen = ref(false)
const categoryDropdownRef = ref<HTMLElement | null>(null)

const toggleCategoryDropdown = () => {
  isCategoryDropdownOpen.value = !isCategoryDropdownOpen.value
}

const selectCategoryFilter = (category: string) => {
  itemCategoryFilter.value = category
  isCategoryDropdownOpen.value = false
}

const getCategoryCount = (category: string) => {
  if (category === 'All') return menuItems.value.length
  return menuItems.value.filter((item) => item.category === category).length
}

const mealsCount = computed(
  () => menuItems.value.filter((item) => isFood(item.category)).length,
)

const beveragesCount = computed(
  () => menuItems.value.filter((item) => isDrink(item.category)).length,
)

const handleClickOutsideCategoryDropdown = (event: MouseEvent) => {
  if (
    categoryDropdownRef.value &&
    !categoryDropdownRef.value.contains(event.target as Node)
  ) {
    isCategoryDropdownOpen.value = false
  }
}

const handleCategoryDropdownKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isCategoryDropdownOpen.value) {
    isCategoryDropdownOpen.value = false
  }
}
const toastMessage = ref('')
const toastType = ref<'success' | 'error'>('success')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const filteredMenuItems = computed(() => {
  return menuItems.value.filter((item) => {
    const matchesTopLevel =
      activeCategory.value === 'Meals' ? isFood(item.category) : isDrink(item.category)
    if (!matchesTopLevel) return false
    return activeSubcategory.value === 'All' || item.category === activeSubcategory.value
  })
})

const menuItem = computed(() => menuItems.value.find((i) => i.id === selectedItemId.value) || null)

const filteredManagerItems = computed(() => {
  const query = itemSearch.value.trim().toLowerCase()
  return menuItems.value.filter((item) => {
    const matchesCategory =
      itemCategoryFilter.value === 'All' || item.category === itemCategoryFilter.value
    const matchesSearch =
      !query || `${item.name} ${item.imageName || ''}`.toLowerCase().includes(query)
    return matchesCategory && matchesSearch
  })
})

const descriptionSearch = ref('')
const descriptionItemId = ref<number | null>(null)
const descriptionDraft = ref('')
const isSavingDescription = ref(false)
const filteredDescriptionItems = computed(() => {
  const query = descriptionSearch.value.trim().toLowerCase()
  return menuItems.value.filter((item) => !query || `${item.name} ${item.category}`.toLowerCase().includes(query))
})
const descriptionItem = computed(
  () => menuItems.value.find((item) => item.id === descriptionItemId.value) || filteredDescriptionItems.value[0] || null,
)

watch(descriptionItem, (item) => {
  descriptionDraft.value = item?.description || ''
  if (item && descriptionItemId.value !== item.id) descriptionItemId.value = item.id
}, { immediate: true })

const saveItemDescription = async () => {
  if (!descriptionItem.value) return
  isSavingDescription.value = true
  try {
    const response = await axios.patch(
      `/api/admin/menu-items/${descriptionItem.value.id}/description`,
      { description: descriptionDraft.value },
    )
    const index = menuItems.value.findIndex((item) => item.id === response.data.id)
    if (index !== -1) menuItems.value[index] = response.data
    showToast('Item description saved.')
  } catch (error) {
    console.error('Failed to save item description:', error)
    showToast('Could not save the description. Please try again.', 'error')
  } finally {
    isSavingDescription.value = false
  }
}

const fetchMenuItems = async () => {
  menuLoadError.value = false
  isLoading.value = true
  try {
    const response = await axios.get('/menu-items')
    menuItems.value = response.data
    if (filteredMenuItems.value.length > 0) {
      await selectItem(filteredMenuItems.value[0].id)
    } else {
      isLoading.value = false
    }
  } catch (error) {
    console.error('Error fetching menu items:', error)
    menuLoadError.value = true
    isLoading.value = false
  }
}

const demographicQuestions = ref(SECTION_1_DEMOGRAPHIC_QUESTIONS)
const dynamicQuestions = ref<any[]>([])
const persistedEvaluationQuestionIds = ref<Set<number>>(new Set())

const hasPersistedOptions = (dbQ: any): boolean => {
  return !!(dbQ && dbQ.id != null)
}

const section2EvaluationQuestions = computed(() => {
  return SECTION_2_EVALUATION_QUESTIONS.map((baseQ) => {
    const dbQ = dynamicQuestions.value.find((q: any) => {
      const t = (q.text || '').toLowerCase()
      if (baseQ.id === 'sec2_mood') return t.includes('mood') || t.includes('emotion') || t.includes('question 1')
      if (baseQ.id === 'sec2_weather') return t.includes('weather') || t.includes('question 2')
      return false
    })

    let title = baseQ.title
    let prompt = baseQ.prompt
    if (dbQ?.text) {
      if (dbQ.text.includes(':')) {
        const parts = dbQ.text.split(':')
        title = parts[0].trim()
        prompt = parts[1].trim()
      } else {
        prompt = dbQ.text.trim()
      }
    }

    let rows: any[] = []
    if (dbQ && dbQ.id != null) {
      rows = (dbQ.options || []).map((opt: any) => ({
        id: opt.id,
        dbId: opt.id,
        label: opt.sub ? `${opt.label} ${opt.sub}` : (opt.icon ? `${opt.icon} ${opt.label}` : opt.label),
        rawLabel: opt.label,
        short: opt.label,
        sub: opt.sub || '',
        icon: opt.icon || '',
      }))
    } else {
      rows = [...baseQ.rows]
    }

    return {
      ...baseQ,
      dbId: dbQ?.id ?? null,
      dbQuestion: dbQ ?? null,
      title,
      prompt,
      rows,
    }
  })
})

const customQuestions = computed(() => {
  return dynamicQuestions.value.filter((q: any) => {
    const txt = (q.text || '').toLowerCase()
    const isMood = txt.includes('mood') || txt.includes('emotion')
    const isWeather = txt.includes('weather')
    const isDemo = txt.includes('age group') || txt.includes('how often do you dine')
    const isLegacyVibe = txt.includes('vibe') || txt.includes('student-friendly') || txt.includes('chatbot')
    return !isMood && !isWeather && !isDemo && !isLegacyVibe
  })
})

const fetchQuestions = async () => {
  try {
    const response = await axios.get('/questions/all')
    dynamicQuestions.value = response.data
  } catch (error) {
    console.error('Error fetching admin questions:', error)
  }
}

const selectItem = async (itemId: number) => {
  selectedItemId.value = itemId
  await fetchCombinedAnalyticsForItem(itemId)
}

const setCategory = (category: string) => {
  activeCategory.value = category
  activeSubcategory.value = 'All'
  autoSelectFirstFilteredItem()
}

const setSubcategory = (subcategory: string) => {
  activeSubcategory.value = subcategory
  autoSelectFirstFilteredItem()
}

const autoSelectFirstFilteredItem = () => {
  if (filteredMenuItems.value.length > 0) {
    const currentStillVisible = filteredMenuItems.value.some((i) => i.id === selectedItemId.value)
    if (!currentStillVisible) selectItem(filteredMenuItems.value[0].id)
  } else {
    analyticsRequestGeneration++
    selectedItemId.value = null
    analyticsData.value = {}
    analyticsLoadError.value = false
    isLoading.value = false
  }
}

const downloadReport = async () => {
  try {
    const response = await axios.get('/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', REPORT_FILENAME)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error('Error downloading report:', error)
    alert('Oops! Could not export the report right now.')
  }
}

const clearAllData = async () => {
  if (isClearingData.value) return
  isClearingData.value = true
  try {
    await axios.delete('/api/admin/clear-data')
    showClearModal.value = false
    showToast('All survey responses deleted.')
    // Refresh in place so the admin stays logged in and sees zeroed stats.
    // Preserve the currently viewed item so its analytics panel reloads
    // with the newly wiped data instead of jumping elsewhere.
    const previousSelection = selectedItemId.value
    await Promise.all([fetchMenuItems(), fetchQuestions(), fetchStats()])
    if (
      previousSelection != null &&
      menuItems.value.some((item) => item.id === previousSelection)
    ) {
      await selectItem(previousSelection)
    }
  } catch (error: any) {
    console.error('Error clearing data:', error)
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      showToast('Session expired. Please log in again.', 'error')
    } else {
      showToast('Could not clear the database. Please try again.', 'error')
    }
  } finally {
    isClearingData.value = false
  }
}

const globalTotal = ref(0)
const itemTotal = ref(0)
const sentiment = ref({ pos: 0, neu: 0, neg: 0, posPct: 0, neuPct: 0, negPct: 0 })

// The backend enforces the real respondent cap and reports it as participantLimit,
// so the card matches what the survey accepts. Fallback (older backend): minimum
// participants needed = (items x per-item target) / items-per-participant.
const globalParticipantTarget = computed(() => {
  const backendLimit = Number(demographics.value?.participantLimit) || 0
  if (backendLimit > 0) return backendLimit
  const required = Math.ceil((menuItems.value.length * SURVEY_BASELINE_TARGET) / SURVEY_ITEM_LIMIT)
  return Math.max(required, SURVEY_BASELINE_TARGET)
})

// Share of all participants who evaluated the current item (itemTotal counts
// distinct evaluators per item, so this is directly comparable to the global
// participant base).
const itemCoverageLabel = computed(() => {
  const itemName = menuItem.value?.name || 'selected item'
  const total = demographics.value?.globalParticipants || baselineCount.value || 0
  if (!total) return `For ${itemName}`
  const pct = Math.min(100, Math.round((itemTotal.value / total) * 100))
  return `For ${itemName} • ${pct}% of ${total} participants`
})

// Votes for one-star rating within a grid row's 1-5 distribution.
const rowVoteCount = (row: any, rating: number): number => {
  return Number(row?.distribution?.[rating] ?? 0)
}

// Full vote-spread sentence for the distribution tooltip.
const distTitle = (row: any): string => {
  const parts = [1, 2, 3, 4, 5].map((n) => {
    const count = rowVoteCount(row, n)
    return `${n}★: ${count} vote${count === 1 ? '' : 's'}`
  })
  return `Vote spread — ${parts.join(' • ')}`
}

const applyItemStats = (data: any) => {
  globalTotal.value = data?.globalTotal ?? 0
  itemTotal.value = data?.itemTotal ?? 0
  sentiment.value = {
    pos: data?.positiveCount ?? 0,
    neu: data?.neutralCount ?? 0,
    neg: data?.negativeCount ?? 0,
    posPct: data?.positivePct ?? 0,
    neuPct: data?.neutralPct ?? 0,
    negPct: data?.negativePct ?? 0,
  }
  topMood.value = data?.topMood || null
  topMoodScore.value = data?.topMoodScore ?? null
  topWeather.value = data?.topWeather || null
  topWeatherScore.value = data?.topWeatherScore ?? null
  avgSuitabilityScore.value = data?.avgSuitabilityScore ?? null
}

const fetchCombinedAnalyticsForItem = async (menuItemId: number) => {
  const generation = ++analyticsRequestGeneration
  isLoading.value = true
  analyticsLoadError.value = false
  try {
    const response = await axios.get(`/analytics/combined/${menuItemId}`)
    if (generation !== analyticsRequestGeneration) return
    analyticsData.value = response.data?.analyticsData || {}
    applyItemStats(response.data?.stats)
    moodAnalytics.value = response.data?.moodAnalytics || null
    weatherAnalytics.value = response.data?.weatherAnalytics || null
    if (response.data?.demographics) {
      demographics.value = response.data.demographics
    }
    recentResponses.value = response.data?.recentResponses || []
  } catch (error) {
    if (generation !== analyticsRequestGeneration) return
    console.error(`Error fetching combined analytics for item ${menuItemId}:`, error)
    analyticsLoadError.value = true
    analyticsData.value = {}
    applyItemStats(null)
    moodAnalytics.value = null
    weatherAnalytics.value = null
    recentResponses.value = []
  } finally {
    if (generation === analyticsRequestGeneration) isLoading.value = false
  }
}

const fetchDemographics = async () => {
  try {
    const response = await axios.get('/analytics/demographics')
    if (response.data) {
      demographics.value = response.data
    }
  } catch (error) {
    console.error('Error fetching demographics:', error)
  }
}

const fetchStats = async () => {
  try {
    const response = await axios.get('/api/stats/baseline')
    baselineCount.value = response.data
  } catch (error) {
    console.error('Error fetching baseline count:', error)
  }
  await fetchDemographics()
}

// Section 1 Demographic Computed Stats & Normalization Helpers
const normalizeDemoKey = (key: string): string => {
  return (key || '')
    .replace(/&#8211;|&ndash;|\u2013|\u2014/g, '-') // Normalize en-dash, em-dash, and HTML entities to standard hyphen
    .replace(/\s*-\s*/g, '-')                       // Normalize spaces around hyphens ('18 - 20' -> '18-20')
    .replace(/\s+/g, ' ')                           // Normalize whitespace
    .trim()
    .toLowerCase()
}

const getAgeGroupCount = (opt: string): number => {
  if (!demographics.value?.ageGroupCounts) return 0
  const target = normalizeDemoKey(opt)
  let sum = 0
  for (const [key, val] of Object.entries(demographics.value.ageGroupCounts)) {
    if (normalizeDemoKey(key) === target) {
      sum += Number(val) || 0
    }
  }
  return sum
}

const getDiningFreqCount = (opt: string): number => {
  if (!demographics.value?.diningFrequencyCounts) return 0
  const target = normalizeDemoKey(opt)
  let sum = 0
  for (const [key, val] of Object.entries(demographics.value.diningFrequencyCounts)) {
    if (normalizeDemoKey(key) === target) {
      sum += Number(val) || 0
    }
  }
  return sum
}

const ageGroupStats = computed(() => {
  const items = AGE_GROUP_OPTIONS.map((opt) => ({
    label: opt,
    count: getAgeGroupCount(opt),
  }))
  const totalAnswers = items.reduce((acc, curr) => acc + curr.count, 0)
  const total = totalAnswers > 0 ? totalAnswers : (demographics.value?.totalParticipants || 0)
  return items.map((item) => ({
    ...item,
    pct: total > 0 ? Math.round((item.count / total) * 100) : 0,
  }))
})

const diningFrequencyStats = computed(() => {
  const items = DINING_FREQUENCY_OPTIONS.map((opt) => ({
    label: opt,
    count: getDiningFreqCount(opt),
  }))
  const totalAnswers = items.reduce((acc, curr) => acc + curr.count, 0)
  const total = totalAnswers > 0 ? totalAnswers : (demographics.value?.totalParticipants || 0)
  return items.map((item) => ({
    ...item,
    pct: total > 0 ? Math.round((item.count / total) * 100) : 0,
  }))
})

// Section 2 Grid Fallback & Display Logic
const parseGridAnalyticsFromRaw = (
  rowDefs: readonly any[],
  title: string,
  prompt: string,
): GridQuestionAnalytics => {
  const accumulators = new Map<
    string,
    { def: any; sum: number; total: number; count4or5: number; distribution: Record<number, number> }
  >()
  rowDefs.forEach((def) => {
    accumulators.set(def.id, {
      def,
      sum: 0,
      total: 0,
      count4or5: 0,
      distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
    })
  })

  const regex = /:\s*([1-5])\b/
  const userSet = new Set<string>()

  Object.values(analyticsData.value).forEach((val) => {
    const list = Array.isArray(val) ? val : []
    list.forEach((item: any) => {
      const text = (item.response || item.textResponse || (typeof item === 'string' ? item : '')).trim()
      if (!text) return
      const match = text.match(regex)
      if (match) {
        const rating = parseInt(match[1], 10)
        const lower = text.toLowerCase()
        for (const def of rowDefs) {
          const shortL = (def.short || def.label).toLowerCase()
          if (lower.startsWith(shortL) || lower.includes(def.id.toLowerCase())) {
            const acc = accumulators.get(def.id)!
            acc.sum += rating
            acc.total++
            if (rating >= 4) acc.count4or5++
            acc.distribution[rating] = (acc.distribution[rating] || 0) + 1
            if (item.userId) userSet.add(item.userId)
            break
          }
        }
      }
    })
  })

  let topLabel: string | null = null
  let topScore = 0
  const rows: GridRowStat[] = []

  accumulators.forEach((acc) => {
    const avg = acc.total > 0 ? Math.round((acc.sum / acc.total) * 10) / 10 : 0
    const suitPct = acc.total > 0 ? Math.round((acc.count4or5 / acc.total) * 100) : 0
    if (avg > topScore) {
      topScore = avg
      topLabel = acc.def.short || acc.def.label
    }
    rows.push({
      id: acc.def.id,
      label: acc.def.label,
      shortLabel: acc.def.short || acc.def.label,
      avgRating: avg,
      totalVotes: acc.total,
      suitabilityPct: suitPct,
      distribution: acc.distribution,
    })
  })

  return {
    title,
    prompt,
    rows,
    topRowLabel: topLabel,
    topRowScore: topScore,
    totalEvaluators: userSet.size || rows.reduce((max, r) => Math.max(max, r.totalVotes), 0),
  }
}

const displayMoodData = computed<GridQuestionAnalytics>(() => {
  if (moodAnalytics.value && moodAnalytics.value.rows && moodAnalytics.value.rows.length > 0) {
    return moodAnalytics.value
  }
  return parseGridAnalyticsFromRaw(
    SECTION_2_MOOD_ROWS,
    'Question 1 — Mood Association',
    'How suitable is this item for each mood?',
  )
})

const displayWeatherData = computed<GridQuestionAnalytics>(() => {
  if (weatherAnalytics.value && weatherAnalytics.value.rows && weatherAnalytics.value.rows.length > 0) {
    return weatherAnalytics.value
  }
  return parseGridAnalyticsFromRaw(
    SECTION_2_WEATHER_ROWS,
    'Question 2 — Weather Association',
    'How suitable is this item for each weather condition?',
  )
})

const displayResponses = computed<SurveyResponseDetail[]>(() => {
  if (recentResponses.value && recentResponses.value.length > 0) {
    return recentResponses.value
  }
  const userMap: Record<string, SurveyResponseDetail> = {}
  const ratingRegex = /:\s*([1-5])\b/
  let anonIndex = 1

  Object.values(analyticsData.value).forEach((val) => {
    const list = Array.isArray(val) ? val : []
    list.forEach((item: any) => {
      const uid = item.userId || `Anon_${anonIndex++}`
      if (!userMap[uid]) {
        userMap[uid] = {
          userId: uid,
          moodRatings: {},
          weatherRatings: {},
          textFeedback: null,
        }
      }
      const text = (item.response || item.textResponse || (typeof item === 'string' ? item : '')).trim()
      if (!text) return
      const match = text.match(ratingRegex)
      if (match) {
        const rating = parseInt(match[1], 10)
        const lower = text.toLowerCase()
        let matched = false
        for (const def of SECTION_2_MOOD_ROWS) {
          if (lower.startsWith(def.short.toLowerCase()) || lower.includes(def.id)) {
            userMap[uid].moodRatings[def.short] = rating
            matched = true
            break
          }
        }
        if (!matched) {
          for (const def of SECTION_2_WEATHER_ROWS) {
            if (lower.startsWith(def.short.toLowerCase()) || lower.includes(def.id)) {
              userMap[uid].weatherRatings[def.short] = rating
              break
            }
          }
        }
      } else {
        userMap[uid].textFeedback = text
      }
    })
  })
  return Object.values(userMap)
})

const getMoodContext = (label: string, shortLabel: string) => {
  if (!label) return ''
  return label.replace(shortLabel, '').replace(/^[\s()–-]+|[\s()–-]+$/g, '')
}

const getWeatherIcon = (shortLabel: string) => {
  if (!shortLabel) return '⛅'
  if (shortLabel.includes('Sunny')) return '☀️'
  if (shortLabel.includes('Humid')) return '🌤️'
  if (shortLabel.includes('Rain')) return '🌧️'
  if (shortLabel.includes('Cool')) return '🍂'
  return '⛅'
}

const getScoreBadgeClass = (rating: number) => {
  if (rating >= 4.0) return 'score-high'
  if (rating >= 3.0) return 'score-med'
  return 'score-low'
}

const getRatingColorClass = (val: number) => {
  if (val >= 4) return 'pill-pos'
  if (val === 3) return 'pill-neu'
  return 'pill-neg'
}

const formatUserId = (userId: string) => {
  if (!userId) return 'Anonymous'
  if (userId.length > 12) return `ID: ...${userId.slice(-6)}`
  return `ID: ${userId}`
}

const hasRatings = (map: any) => {
  return map && typeof map === 'object' && Object.keys(map).length > 0
}

let securityInterceptor: number | null = null

const activeAdminTab = ref('analytics')

// ==========================================
// TAB 5: STATISTICAL TESTING STATE & LOGIC
// ==========================================
const statisticalOverview = ref<any>(null)
const tTestResult = ref<any>(null)
const anovaResult = ref<any>(null)
const statsLoading = ref(false)
const activeTestType = ref<'ttest' | 'anova'>('ttest')

const tTestMode = ref('SUPER_CATEGORIES')
const tTestGroup1 = ref('')
const tTestGroup2 = ref('')
const tTestDimension = ref('all')
const tTestAlpha = ref(0.05)

const anovaFactor = ref('SUBCATEGORIES')
const anovaDimension = ref('all')
const anovaItemId = ref<number | null>(null)
const anovaAlpha = ref(0.05)

const availableDimensionOptions = computed(() => {
  if (statisticalOverview.value?.dimensionOptions?.length) {
    return statisticalOverview.value.dimensionOptions.map((d: any) => ({
      key: d.key,
      label: d.label,
      icon: d.icon || '',
      questionText: d.questionText || '',
    }))
  }
  if (statisticalOverview.value?.dimensions?.length) {
    return statisticalOverview.value.dimensions
      .filter((d: string) => !d.startsWith('All'))
      .map((d: string) => ({
        key: d.toLowerCase(),
        label: d,
        icon: '',
        questionText: '',
      }))
  }
  return []
})

const isDimensionAnovaFactor = computed(() => {
  const f = anovaFactor.value
  return f === 'MOOD_DIMENSIONS' ||
    f === 'WEATHER_DIMENSIONS' ||
    f === 'ALL_DIMENSIONS' ||
    f.startsWith('QUESTION_')
})

const tTestGroup1Options = computed(() => {
  if (!statisticalOverview.value) return []
  if (tTestMode.value === 'SUBCATEGORIES') {
    return (statisticalOverview.value.subcategories || []).map((sc: string) => ({ value: sc, label: sc }))
  }
  if (tTestMode.value === 'ITEMS') {
    return (statisticalOverview.value.items || []).map((it: any) => ({
      value: String(it.id),
      label: `${it.name} (${it.category})`,
    }))
  }
  if (tTestMode.value === 'AGE_GROUPS') {
    return (statisticalOverview.value.ageGroups || []).map((ag: string) => ({ value: ag, label: ag }))
  }
  if (tTestMode.value === 'DINING_FREQUENCY') {
    return (statisticalOverview.value.diningFrequencies || []).map((df: string) => ({ value: df, label: df }))
  }
  if (tTestMode.value === 'DIMENSIONS') {
    return availableDimensionOptions.value.map((d: any) => ({
      value: d.key,
      label: d.icon ? `${d.icon} ${d.label}` : d.label,
    }))
  }
  return []
})

const tTestGroup2Options = computed(() => {
  return tTestGroup1Options.value.filter((opt: any) => opt.value !== tTestGroup1.value)
})

const onTTestModeChange = () => {
  const opts = tTestGroup1Options.value
  if (opts.length >= 2) {
    tTestGroup1.value = opts[0].value
    tTestGroup2.value = opts[1].value
  } else if (opts.length === 1) {
    tTestGroup1.value = opts[0].value
    tTestGroup2.value = ''
  } else {
    tTestGroup1.value = ''
    tTestGroup2.value = ''
  }
}

const fetchStatisticalOverview = async () => {
  statsLoading.value = true
  try {
    const res = await axios.get('/analytics/statistical-tests/overview')
    statisticalOverview.value = res.data
    if (res.data.sampleTTest && !tTestResult.value) {
      tTestResult.value = res.data.sampleTTest
    }
    if (res.data.sampleAnova && !anovaResult.value) {
      anovaResult.value = res.data.sampleAnova
    }
    onTTestModeChange()
  } catch (error) {
    console.error('Error fetching statistical overview:', error)
    showToast('Failed to load statistical overview.', 'error')
  } finally {
    statsLoading.value = false
  }
}

const executeTTest = async () => {
  statsLoading.value = true
  try {
    const payload = {
      mode: tTestMode.value,
      group1: tTestMode.value === 'SUPER_CATEGORIES' ? 'Meals' : tTestGroup1.value,
      group2: tTestMode.value === 'SUPER_CATEGORIES' ? 'Beverages' : tTestGroup2.value,
      dimension: tTestDimension.value,
      alpha: tTestAlpha.value,
    }
    const res = await axios.post('/analytics/statistical-tests/t-test', payload)
    tTestResult.value = res.data
    showToast('T-Test calculated successfully.')
  } catch (error) {
    console.error('Error calculating T-Test:', error)
    showToast('Could not calculate T-Test.', 'error')
  } finally {
    statsLoading.value = false
  }
}

const executeAnova = async () => {
  statsLoading.value = true
  try {
    const payload = {
      factor: anovaFactor.value,
      dimension: anovaDimension.value,
      menuItemId: anovaItemId.value,
      alpha: anovaAlpha.value,
    }
    const res = await axios.post('/analytics/statistical-tests/anova', payload)
    anovaResult.value = res.data
    showToast('ANOVA calculated successfully.')
  } catch (error) {
    console.error('Error calculating ANOVA:', error)
    showToast('Could not calculate ANOVA.', 'error')
  } finally {
    statsLoading.value = false
  }
}

watch(activeAdminTab, (newTab) => {
  if (newTab === 'statistics' && !statisticalOverview.value) {
    fetchStatisticalOverview()
  }
})

const showItemModal = ref(false)
const isSavingItem = ref(false)
const showLogoutModal = ref(false)
const showAddOptionModal = ref(false)
const isSavingOption = ref(false)
const showDeleteQuestionModal = ref(false)
const showDeleteItemModal = ref(false)
const showDeleteAllModal = ref(false)
const isDeletingItem = ref(false)
const isDeletingAll = ref(false)
const questionToDelete = ref<number | null>(null)
const questionToDeleteText = ref('')
const itemToDelete = ref<number | null>(null)
const itemEditSnapshot = ref('')

const editingItem = ref({
  id: null as number | null,
  name: '',
  category: 'APPETIZER',
  imageName: '',
})

const imageFileInput = ref<HTMLInputElement | null>(null)
const isUploadingImage = ref(false)
const imageError = ref('')
const localPreviewUrl = ref<string | null>(null)

const imagePreviewUrl = computed(() => {
  if (localPreviewUrl.value) return localPreviewUrl.value
  if (editingItem.value.imageName) return getImagePath({ imageName: editingItem.value.imageName })
  return ''
})

const triggerImagePicker = () => {
  imageError.value = ''
  imageFileInput.value?.click()
}

const clearImage = () => {
  editingItem.value.imageName = ''
  imageError.value = ''
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = null
  }
  if (imageFileInput.value) imageFileInput.value.value = ''
}

const onImageFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  imageError.value = ''
  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg']
  if (!allowed.includes(file.type)) {
    imageError.value = 'Use PNG, JPG or WEBP.'
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    imageError.value = 'File too large (max 5MB).'
    input.value = ''
    return
  }

  if (localPreviewUrl.value) URL.revokeObjectURL(localPreviewUrl.value)
  localPreviewUrl.value = URL.createObjectURL(file)

  isUploadingImage.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await axios.post('/api/admin/menu-items/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    editingItem.value.imageName = res.data.imageName || res.data.url || ''
    // keep preview, but now backed by persisted file; clear object URL will be replaced by getImagePath after next edit
  } catch (e: any) {
    imageError.value = e?.response?.data?.error || 'Upload failed. Try again.'
    if (localPreviewUrl.value) {
      URL.revokeObjectURL(localPreviewUrl.value)
      localPreviewUrl.value = null
    }
  } finally {
    isUploadingImage.value = false
    input.value = ''
  }
}

const openNewItemModal = () => {
  clearImage()
  editingItem.value = { id: null, name: '', category: 'APPETIZER', imageName: '' }
  itemEditSnapshot.value = JSON.stringify(editingItem.value)
  showItemModal.value = true
}

const openEditModal = (item: any) => {
  clearImage()
  editingItem.value = { ...item }
  itemEditSnapshot.value = JSON.stringify(editingItem.value)
  showItemModal.value = true
}

const itemPendingDelete = computed(
  () => menuItems.value.find((item) => item.id === itemToDelete.value) || null,
)

const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  toastMessage.value = message
  toastType.value = type
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 3500)
}

const closeItemModal = () => {
  const hasUnsavedChanges = JSON.stringify(editingItem.value) !== itemEditSnapshot.value
  if (hasUnsavedChanges && !window.confirm('Discard your unsaved changes?')) {
    return
  }
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = null
  }
  imageError.value = ''
  showItemModal.value = false
}

const saveMenuItem = async () => {
  isSavingItem.value = true
  try {
    if (editingItem.value.id) {
      await axios.put(`/api/admin/menu-items/${editingItem.value.id}`, editingItem.value)
    } else {
      await axios.post('/api/admin/menu-items', editingItem.value)
    }

    await fetchMenuItems()
    if (localPreviewUrl.value) {
      URL.revokeObjectURL(localPreviewUrl.value)
      localPreviewUrl.value = null
    }
    showItemModal.value = false
    showToast(editingItem.value.id ? 'Menu item updated.' : 'Menu item added.')
  } catch (error) {
    console.error('Failed to save item:', error)
    showToast('Could not save the item. Please try again.', 'error')
  } finally {
    isSavingItem.value = false
  }
}

const showQuestionModal = ref(false)
const isSavingQuestion = ref(false)

const editingQuestion = ref({
  id: null as number | null,
  text: '',
  type: 'RADIO',
})

const openNewQuestionModal = () => {
  editingQuestion.value = { id: null, text: '', type: 'RADIO' }
  showQuestionModal.value = true
}

const openEditQuestionModal = (q: any) => {
  editingQuestion.value = { id: q.id, text: q.text, type: q.type || q.questionType || 'RADIO' }
  showQuestionModal.value = true
}

const openEditEvaluationQuestionModal = async (q: any) => {
  let targetQId = q.dbId
  if (!targetQId) {
    try {
      const res = await axios.post('/api/admin/questions', {
        text: `${q.title}: ${q.prompt}`,
        type: 'MATRIX',
      })
      targetQId = res.data.id
      await fetchQuestions()
    } catch (e) {
      console.error('Failed to initialize evaluation question in DB:', e)
      alert('Could not initialize question in database.')
      return
    }
  }

  editingQuestion.value = {
    id: targetQId,
    text: q.dbQuestion?.text || `${q.title}: ${q.prompt}`,
    type: 'MATRIX',
  }
  showQuestionModal.value = true
}

const saveQuestion = async () => {
  isSavingQuestion.value = true
  try {
    const payload = { text: editingQuestion.value.text, type: editingQuestion.value.type }
    if (editingQuestion.value.id) {
      await axios.put(`/api/admin/questions/${editingQuestion.value.id}`, payload)
    } else {
      await axios.post('/api/admin/questions', payload)
    }

    await fetchQuestions()
    showQuestionModal.value = false
  } catch (error) {
    console.error('Failed to save question: ', error)
    alert('Error saving question.')
  } finally {
    isSavingQuestion.value = false
  }
}

const confirmDeleteItem = (item: any) => {
  itemToDelete.value = item.id
  showDeleteItemModal.value = true
}

const executeDeleteItem = async () => {
  if (!itemToDelete.value) return

  isDeletingItem.value = true
  try {
    const deletedName = itemPendingDelete.value?.name || 'Menu item'
    await axios.delete(`/api/admin/menu-items/${itemToDelete.value}`)
    await fetchMenuItems()

    showDeleteItemModal.value = false
    itemToDelete.value = null
    showToast(`${deletedName} deleted.`)
  } catch (error) {
    console.error('Failed to delete menu item:', error)
    showToast('Could not delete the menu item. Please try again.', 'error')
  } finally {
    isDeletingItem.value = false
  }
}

const executeDeleteAllItems = async () => {
  if (menuItems.value.length === 0) return
  isDeletingAll.value = true
  try {
    await axios.delete('/api/admin/menu-items', {
      headers: {
        Authorization: axios.defaults.headers.common['Authorization'] as string,
      },
      // custom flag consumed by the response interceptor to skip the auth redirect
      skipAuthRedirect: true,
    } as any)
    await fetchMenuItems()
    showDeleteAllModal.value = false
    itemToDelete.value = null
    selectedItemId.value = null
    analyticsData.value = {}
    showToast(`Deleted all menu items.`)
  } catch (error: any) {
    console.error('Failed to delete all menu items:', error, error?.response?.data)
    const status = error?.response?.status
    const msg = error?.response?.data?.error || error?.response?.data?.message || (typeof error?.response?.data === 'string' ? error?.response?.data : null)
    if (status === 401 || status === 403) {
      showToast('Session expired. Please log in again.', 'error')
    } else if (msg) {
      showToast(msg, 'error')
    } else {
      showToast(`Delete failed (${status ?? 'network error'}). Check console for details.`, 'error')
    }
  } finally {
    isDeletingAll.value = false
  }
}

const confirmDeleteQuestion = (id: number, text: string) => {
  questionToDelete.value = id // Remember which question we are deleting
  questionToDeleteText.value = text // Remember the text to show in the modal
  showDeleteQuestionModal.value = true // Open the custom modal
}

const executeDeleteQuestion = async () => {
  if (!questionToDelete.value) return

  try {
    await axios.delete(`/api/admin/questions/${questionToDelete.value}`)
    await fetchQuestions() // Refresh the list
    showDeleteQuestionModal.value = false // Close modal
    showToast('Question deleted successfully.')
    questionToDelete.value = null // Clear memory
    questionToDeleteText.value = '' // Clear text
  } catch (error) {
    console.error('Failed to delete question:', error)
    showToast('Could not delete question. Please try again.', 'error')
  }
}


const quickEmojis = QUICK_EMOJIS

const optionForm = ref({
  questionId: null as number | null,
  label: '',
  icon: '',
})

const openAddOptionModal = (questionId: number) => {
  optionForm.value = { questionId: questionId, label: '', icon: '🔘' } // Reset form
  showAddOptionModal.value = true
}

const submitNewOption = async () => {
  if (!optionForm.value.questionId || !optionForm.value.label) return

  isSavingOption.value = true
  try {
    await axios.post(`/api/admin/questions/${optionForm.value.questionId}/options`, {
      label: optionForm.value.label,
      icon: optionForm.value.icon,
    })
    await fetchQuestions()
    showAddOptionModal.value = false
  } catch (error) {
    console.error('Failed to add option:', error)
    alert('Error saving option.')
  } finally {
    isSavingOption.value = false
  }
}

const showDeleteOptionModal = ref(false)
const optionToDelete = ref<number | null>(null)

const confirmDeleteOption = (optionId: number) => {
  optionToDelete.value = optionId
  showDeleteOptionModal.value = true
}

const executeDeleteOption = async () => {
  if (!optionToDelete.value) return

  try {
    await axios.delete(`/api/admin/options/${optionToDelete.value}`)
    await fetchQuestions()

    showDeleteOptionModal.value = false
    optionToDelete.value = null
  } catch (error) {
    console.error('Failed to delete option:', error)
    alert('Could not delete option.')
  }
}

// --- EVALUATION DIMENSIONS CRUD ---
const showDimensionModal = ref(false)
const isSavingDimension = ref(false)
const dimensionForm = ref({
  id: null as number | null,
  questionId: null as number | null,
  label: '',
  sub: '',
  icon: '',
  q: null as any,
  rawRow: null as any,
})

const deleteDimensionFromModal = () => {
  if (!dimensionForm.value) return
  const q = dimensionForm.value.q
  const rawRow = dimensionForm.value.rawRow || {
    id: dimensionForm.value.id,
    label: dimensionForm.value.label,
  }
  showDimensionModal.value = false
  confirmDeleteDimension(q, rawRow)
}

const openAddDimensionModal = async (q: any) => {
  let targetQId = q.dbId
  const hasUnpersistedRows = q.rows && q.rows.some((r: any) => !r.dbId)

  if (!targetQId || hasUnpersistedRows) {
    try {
      if (!targetQId) {
        const payload = {
          text: `${q.title}: ${q.prompt}`,
          type: 'MATRIX',
        }
        const res = await axios.post('/api/admin/questions', payload)
        targetQId = res.data.id
      }
      if (hasUnpersistedRows) {
        for (const r of q.rows) {
          if (!r.dbId) {
            await axios.post(`/api/admin/questions/${targetQId}/options`, {
              label: r.short || r.rawLabel || r.label,
              sub: r.sub || (r.label.includes('(') ? r.label.substring(r.label.indexOf('(')) : ''),
              icon: r.icon || null,
            })
          }
        }
      }
      if (targetQId) {
        persistedEvaluationQuestionIds.value.add(targetQId)
      }
      await fetchQuestions()
    } catch (e) {
      console.error('Failed to create evaluation question in DB:', e)
      alert('Could not initialize question in database.')
      return
    }
  } else if (targetQId) {
    persistedEvaluationQuestionIds.value.add(targetQId)
  }

  dimensionForm.value = {
    id: null,
    questionId: targetQId,
    label: '',
    sub: '',
    icon: '',
    q,
    rawRow: null,
  }
  showDimensionModal.value = true
}

const openEditDimensionModal = async (q: any, row: any) => {
  let targetQId = q.dbId
  let rowDbId = row.dbId || (typeof row.id === 'number' ? row.id : null)

  if (!targetQId || !rowDbId) {
    try {
      if (!targetQId) {
        const res = await axios.post('/api/admin/questions', {
          text: `${q.title}: ${q.prompt}`,
          type: 'MATRIX',
        })
        targetQId = res.data.id
      }
      if (!rowDbId) {
        for (const r of q.rows) {
          const optRes = await axios.post(`/api/admin/questions/${targetQId}/options`, {
            label: r.short || r.rawLabel || r.label,
            sub: r.sub || (r.label.includes('(') ? r.label.substring(r.label.indexOf('(')) : ''),
            icon: r.icon || null,
          })
          if (r.id === row.id) {
            rowDbId = optRes.data.id
          }
        }
        await fetchQuestions()
      }
    } catch (e) {
      console.error('Failed to sync options to DB:', e)
      alert('Could not sync dimensions to database.')
      return
    }
  }

  if (targetQId) {
    persistedEvaluationQuestionIds.value.add(targetQId)
  }

  dimensionForm.value = {
    id: rowDbId,
    questionId: targetQId,
    label: row.rawLabel || row.short || row.label.replace(/\s*\(.*?\)/, '').trim(),
    sub: row.sub || (row.label.includes('(') ? row.label.substring(row.label.indexOf('(')) : ''),
    icon: row.icon || '',
    q,
    rawRow: row,
  }
  showDimensionModal.value = true
}

const saveDimension = async () => {
  if (!dimensionForm.value.label.trim()) return
  isSavingDimension.value = true
  try {
    const payload = {
      label: dimensionForm.value.label.trim(),
      sub: dimensionForm.value.sub.trim() || null,
      icon: dimensionForm.value.icon.trim() || null,
    }

    if (dimensionForm.value.id) {
      await axios.put(`/api/admin/options/${dimensionForm.value.id}`, payload)
      showToast('Dimension updated successfully!')
    } else {
      await axios.post(`/api/admin/questions/${dimensionForm.value.questionId}/options`, payload)
      showToast('New dimension added successfully!')
    }

    if (dimensionForm.value.questionId) {
      persistedEvaluationQuestionIds.value.add(dimensionForm.value.questionId)
    }

    await fetchQuestions()
    showDimensionModal.value = false
  } catch (error) {
    console.error('Failed to save dimension:', error)
    alert('Failed to save dimension.')
  } finally {
    isSavingDimension.value = false
  }
}

const showDeleteDimensionModal = ref(false)
const isDeletingDimension = ref(false)
const dimensionToDelete = ref<{
  id: number | null
  rawId: string | number
  label: string
  questionId: number | null
  q: any
  row: any
} | null>(null)

const confirmDeleteDimension = (q: any, row: any) => {
  const rowDbId = row.dbId || (typeof row.id === 'number' ? row.id : null)
  dimensionToDelete.value = {
    id: rowDbId,
    rawId: row.id,
    label: row.rawLabel || row.short || row.label,
    questionId: q.dbId,
    q,
    row,
  }
  showDeleteDimensionModal.value = true
}

const executeDeleteDimension = async () => {
  if (!dimensionToDelete.value) return
  isDeletingDimension.value = true
  try {
    const { id, q, row, label } = dimensionToDelete.value
    let targetQId = q?.dbId || q?.dbQuestion?.id || (typeof q?.id === 'number' ? q.id : null)

    if (id) {
      await axios.delete(`/api/admin/options/${id}`)
      if (targetQId) {
        persistedEvaluationQuestionIds.value.add(targetQId)
      }
      showToast(`Deleted dimension "${label}".`)
      await fetchQuestions()
    } else {
      if (!targetQId) {
        const qRes = await axios.post('/api/admin/questions', {
          text: `${q.title}: ${q.prompt}`,
          type: 'MATRIX',
        })
        targetQId = qRes.data.id
      }

      for (const r of q.rows) {
        if (r.id === row.id) continue
        await axios.post(`/api/admin/questions/${targetQId}/options`, {
          label: r.short || r.rawLabel || r.label,
          sub: r.sub || (r.label.includes('(') ? r.label.substring(r.label.indexOf('(')) : ''),
          icon: r.icon || null,
        })
      }

      if (targetQId) {
        persistedEvaluationQuestionIds.value.add(targetQId)
      }
      showToast(`Deleted dimension "${label}".`)
      await fetchQuestions()
    }

    showDeleteDimensionModal.value = false
    dimensionToDelete.value = null
  } catch (error) {
    console.error('Failed to delete dimension:', error)
    alert('Could not delete dimension.')
  } finally {
    isDeletingDimension.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutsideCategoryDropdown)
  securityInterceptor = axios.interceptors.response.use(
    (response) => response,
    (error) => {
      const url = (error.config?.url as string) || ''
      const isDeleteAll =
        url === '/api/admin/menu-items' && (error.config?.method as string)?.toLowerCase() === 'delete'
      const skipRedirect =
        (error.config as any)?.skipAuthRedirect === true ||
        error.config?.headers?.['X-Skip-Auth-Redirect'] === 'true' ||
        isDeleteAll
      if (!skipRedirect && error.response && (error.response.status === 401 || error.response.status === 403)) {
        console.warn('Session expired! Returning to login screen...')
        delete axios.defaults.headers.common['Authorization']
        try {
          window.localStorage.removeItem('admin_token')
        } catch {
          // Continue clearing the in-memory session when browser storage is unavailable.
        }
        isAuthenticated.value = false
      }
      return Promise.reject(error)
    },
  )

  if (storedAdminToken) {
    void (async () => {
      try {
        const response = await axios.get('/api/admin/session')
        if (response.data?.authenticated !== true) throw new Error('Invalid admin session')
        isAuthenticated.value = true
        await Promise.all([fetchMenuItems(), fetchQuestions(), fetchStats()])
      } catch {
        delete axios.defaults.headers.common['Authorization']
        try { window.localStorage.removeItem('admin_token') } catch { /* Storage unavailable. */ }
        isAuthenticated.value = false
      }
    })()
  }
})

onUnmounted(() => {
  analyticsRequestGeneration++
  document.removeEventListener('click', handleClickOutsideCategoryDropdown)
  delete axios.defaults.headers.common['Authorization']
  if (securityInterceptor != null) {
    axios.interceptors.response.eject(securityInterceptor)
  }
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = null
  }
})
</script>

<style scoped>
/* LOGIN SCREEN */
.login-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f4f7fa;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  width: 100%;
  max-width: 400px;
  text-align: center;
  border: 1px solid #e2e8f0;
}

.login-icon {
  font-size: 3rem;
  margin-bottom: 10px;
}
.login-card h2 {
  margin: 0 0 10px 0;
  color: #0f172a;
  font-size: 1.8rem;
}
.login-card p {
  color: #64748b;
  margin-bottom: 25px;
  font-size: 0.95rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.login-input {
  padding: 12px 15px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 1rem;
  background: #f8fafc;
  transition: border-color 0.2s;
}
.login-input:focus {
  outline: none;
  border-color: #f97316;
  background: white;
}

.login-btn {
  background: #0f172a;
  color: white;
  border: none;
  padding: 14px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 10px;
  transition: background 0.2s;
}
.login-btn:hover:not(:disabled) {
  background: #1e293b;
}
.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-text {
  color: #ef4444 !important;
  font-size: 0.85rem !important;
  margin: 0 !important;
  text-align: left;
}

/* GLOBALS & HEADER */
.dashboard-layout {
  background-color: #f4f7fa;
  min-height: 100vh;
  padding: 30px 50px;
  font-family:
    'Inter',
    -apple-system,
    sans-serif;
  color: #1e293b;
}
.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}
.logo-icon {
  background: #f97316;
  color: white;
  width: 45px;
  height: 45px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}
.top-header h1 {
  margin: 0 0 5px 0;
  font-size: 1.5rem;
  color: #0f172a;
}
.subtitle {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}
.header-right {
  display: flex;
  gap: 15px;
  align-items: center;
}
.live-badge {
  background: #ecfdf5;
  color: #10b981;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #a7f3d0;
}
.live-badge .dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
}

.export-btn {
  background: #1e293b;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.export-btn:hover {
  background: #0f172a;
}
.danger-btn {
  background: #ef4444;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.danger-btn:hover {
  background: #dc2626;
}

.truncate-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* SLEEK REDESIGNED KPI CARDS */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 25px;
}

.new-kpi-card {
  border: 1px solid #e2ded5;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.global-card {
  background-color: #eff6ff;
  border-left: 3px solid #3b82f6;
}

.item-card {
  background-color: #ffffff;
  border-left: 3px solid #f07000;
}

.scope-label {
  font-size: 10px;
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.global-card .scope-label {
  color: #3b82f6;
}
.item-card .scope-label {
  color: #f07000;
}

.scope-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.blue-dot {
  background-color: #3b82f6;
}
.orange-dot {
  background-color: #f07000;
}

.kpi-val {
  margin: 0 0 4px 0;
  font-size: 42px;
  font-weight: 800;
  color: #1c1a17;
  line-height: 1;
}

/* NOTE: kpi-val-wrapper / keyword-pill were removed with the TOP MOOD and
   TOP WEATHER KPI cards (that info already lives in the item summary and the
   Key Insight footers). */

.kpi-name {
  margin: 0 0 8px 0;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
  font-weight: 600;
}

.kpi-desc {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

/* OPTION 1 NAVIGATION PANEL */
.navigation-panel {
  background: white;
  border-radius: 16px;
  margin-bottom: 30px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}
.filters-row {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
  gap: 20px;
}
.category-toggle {
  position: relative;
  display: inline-grid;
  grid-template-columns: 1fr 1fr;
  background: #e2e8f0;
  padding: 4px;
  border-radius: 12px;
  user-select: none;
}
.category-toggle button {
  position: relative;
  z-index: 2;
  background: transparent !important;
  border: none;
  padding: 8px 24px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.category-toggle button.active {
  color: #0f172a;
}
.category-toggle .sliding-bg {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  z-index: 1;
}
.category-toggle .sliding-bg.slide-left {
  transform: translateX(0%);
}
.category-toggle .sliding-bg.slide-right {
  transform: translateX(100%);
}
.divider {
  width: 1px;
  height: 24px;
  background: #cbd5e1;
}
.subcategory-pills {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.subcategory-pills::-webkit-scrollbar {
  display: none;
}
.f-pill {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  border: 1px solid;
}

.pill-default {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #475569;
}
.pill-appetizer {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}
.pill-appetizer:hover {
  background: #ffedd5;
}
.pill-appetizer.active {
  background: #fdba74;
  color: #7c2d12;
  border-color: #f97316;
}
.pill-pasta {
  background: #fefce8;
  border-color: #fde68a;
  color: #854d0e;
}
.pill-pasta:hover {
  background: #fef9c3;
}
.pill-pasta.active {
  background: #facc15;
  color: #422006;
  border-color: #eab308;
}
.pill-sandwich {
  background: #fdf5e6;
  border-color: #ebd5b3;
  color: #7c2d12;
}
.pill-sandwich:hover {
  background: #faebd7;
}
.pill-sandwich.active {
  background: #deb887;
  color: #5c3317;
  border-color: #cdaa7d;
}
.pill-wings {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}
.pill-wings:hover {
  background: #fee2e2;
}
.pill-wings.active {
  background: #fca5a5;
  color: #7f1d1d;
  border-color: #f87171;
}
.pill-ricemeal {
  background: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}
.pill-ricemeal:hover {
  background: #fef3c7;
}
.pill-ricemeal.active {
  background: #fcd34d;
  color: #78350f;
  border-color: #f59e0b;
}
.pill-classics {
  background: #f5f3ff;
  border-color: #ddd6fe;
  color: #5b21b6;
}
.pill-classics:hover {
  background: #ede9fe;
}
.pill-classics.active {
  background: #c4b5fd;
  color: #5b21b6;
  border-color: #8b5cf6;
}
.pill-iceblended {
  background: #ecfeff;
  border-color: #a5f3fc;
  color: #155e75;
}
.pill-iceblended:hover {
  background: #cffafe;
}
.pill-iceblended.active {
  background: #67e8f9;
  color: #164e63;
  border-color: #06b6d4;
}
.pill-specialty {
  background: #fdf2f8;
  border-color: #fbcfe8;
  color: #9d174d;
}
.pill-specialty:hover {
  background: #fce7f3;
}
.pill-specialty.active {
  background: #f9a8d4;
  color: #831843;
  border-color: #f472b6;
}
.pill-noncoffee {
  background: #f0f9ff;
  border-color: #bae6fd;
  color: #0c4a6e;
}
.pill-noncoffee:hover {
  background: #e0f2fe;
}
.pill-noncoffee.active {
  background: #7dd3fc;
  color: #0369a1;
  border-color: #0ea5e9;
}
.pill-refresher {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
}
.pill-refresher:hover {
  background: #d1fae5;
}
.pill-refresher.active {
  background: #6ee7b7;
  color: #064e3b;
  border-color: #10b981;
}
.pill-matcha {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #14532d;
}
.pill-matcha:hover {
  background: #dcfce7;
}
.pill-matcha.active {
  background: #86efac;
  color: #14532d;
  border-color: #22c55e;
}

.item-tabs-container {
  display: flex;
  gap: 5px;
  padding: 15px 20px;
  overflow-x: auto;
}
.empty-filter {
  padding: 20px;
  color: #94a3b8;
  font-style: italic;
  font-size: 0.9rem;
}
.item-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
  padding: 10px;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
  opacity: 0.6;
}
.item-tab:hover {
  opacity: 0.9;
}
.item-tab:focus-visible {
  outline: 3px solid #0f766e;
  outline-offset: 2px;
}
.item-tab.active {
  opacity: 1;
  border-bottom-color: #f97316;
}
.tab-thumb {
  width: 45px;
  height: 45px;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
  border: 1px solid #e2e8f0;
  margin-bottom: 8px;
}
.tab-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #0f172a;
  max-width: 110px;
  text-align: center;
}
.item-tab.active .tab-title {
  color: #ea580c;
}
.tab-cat {
  font-size: 0.7rem;
  color: #94a3b8;
}
.item-tab.active .tab-cat {
  color: #fb923c;
}

/* STATES */
.state-message {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
}
.state-message.empty h2 {
  color: #0f172a;
  font-size: 2rem;
  margin-bottom: 10px;
}
.state-message.empty p {
  font-size: 1.1rem;
}
.badge-v2 {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  display: inline-block;
  border: 1px solid transparent;
}
.badge-v2.food-badge {
  background: #ffedd5;
  color: #c2410c;
}
.badge-v2.type-badge {
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
}
.q-circle {
  position: absolute;
  bottom: 15px;
  right: 15px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  z-index: 3;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* RADIO CARDS */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
}
.insight-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #e2e8f0;
}

/* DYNAMIC CSS VARIABLES FOR CHARTS */
.radio-card-v2 {
  border-top: 4px solid var(--c-main, #f97316);
}
.text-card-v2 {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  border-top: 4px solid var(--c-main, #f97316);
}

.card-image-header-v2 {
  height: 180px;
  background-color: #e2e8f0;
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 20px;
}
.card-image-header-v2::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 70%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  z-index: 1;
}
.image-overlay-v2 {
  z-index: 2;
  width: 100%;
}
.image-overlay-v2 h3 {
  color: white;
  margin: 0 0 8px 0;
  font-size: 1.3rem;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}
.card-body {
  padding: 25px;
}
.insight-body {
  display: block;
}
.question-title {
  margin: 0 0 5px 0;
  font-size: 1.1rem;
  color: #0f172a;
}
.response-count {
  margin: 0 0 20px 0;
  font-size: 0.85rem;
  color: #94a3b8;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 12px;
}
.bar-label {
  width: 100px;
  font-size: 0.9rem;
  color: #475569;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* DYNAMIC BAR GRAPHS */
.bar-track-v2 {
  flex-grow: 1;
  height: 28px;
  background: var(--c-light, #fff7ed);
  border-radius: 8px;
  overflow: hidden;
}
.bar-fill.orange-solid {
  height: 100%;
  border-radius: 8px;
  transition: width 1s ease;
  background: var(--c-main, #f97316);
}

.bar-value {
  width: 30px;
  font-weight: 600;
  font-size: 0.9rem;
  color: #0f172a;
  text-align: right;
}
.bar-percent {
  width: 40px;
  font-size: 0.85rem;
  color: #94a3b8;
  text-align: right;
}

/* DYNAMIC INSIGHT BOX */
.key-insight-v2 {
  margin-top: 25px;
  background: var(--c-light, #fff7ed);
  padding: 12px 15px;
  border-radius: 8px;
  color: var(--c-text, #c2410c);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* NOTE: The legacy TEXT CARDS / sentiment-panel / word-cloud layout was removed.
   The analytics view now shows sentiment in the KPI strip, vote spread per grid
   row, demographics, and the individual-responses log instead. */

/* Excerpts */
.text-bottom-row {
  padding: 25px;
  background: white;
}
.excerpt-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-left-side {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mb-0 {
  margin-bottom: 0 !important;
}
.excerpt-count {
  font-size: 0.8rem;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.excerpt-filters {
  display: flex;
  gap: 8px;
}
.f-btn {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background: white;
  color: #64748b;
  transition: all 0.2s;
}
.f-btn:hover {
  background: #f8fafc;
}
.f-btn.active {
  background: #0f172a;
  color: white;
  border-color: #0f172a;
}
.pos-btn.active {
  background: #16a34a !important;
  color: white !important;
  border-color: #16a34a !important;
}
.neu-btn.active {
  background: #64748b !important;
  color: white !important;
  border-color: #64748b !important;
}
.neg-btn.active {
  background: #ef4444 !important;
  color: white !important;
  border-color: #ef4444 !important;
}
.excerpt-grid-v2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}
.exc-card {
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid #e2e8f0;
}
.exc-text {
  margin: 0 0 15px 0;
  font-size: 0.9rem;
  color: #334155;
  line-height: 1.5;
}
.exc-footer {
  display: flex;
}
.exc-tag {
  font-size: 0.8rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}
.exc-tag.pos {
  color: #16a34a;
}
.exc-tag.neu {
  color: #64748b;
}
.exc-tag.neg {
  color: #ef4444;
}

/* MODAL STYLES */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.modal-card {
  background: white;
  padding: 40px;
  border-radius: 24px;
  text-align: center;
  max-width: 450px;
  width: 90%;
  margin: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}
.modal-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  margin: 0 auto 15px auto;
}
.modal-card h2 {
  margin: 0 0 15px 0;
  color: #0f172a;
  font-size: 1.8rem;
  font-weight: 800;
}
.modal-card p {
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 30px;
  font-size: 1.05rem;
}
.modal-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 10px;
}
.nav-btn {
  padding: 14px 24px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  flex: 1;
}
.nav-btn.secondary {
  background: white;
  border: 1px solid #cbd5e1;
  color: #475569;
}
.nav-btn.secondary:hover {
  background: #f1f5f9;
}
.danger-solid {
  background: #ef4444;
  color: white;
}
.danger-solid:hover {
  background: #dc2626;
}
@keyframes popIn {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* LOGOUT BUTTON STYLES */
.logout-btn {
  background-color: #64748b;
  color: white;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition:
    background-color 0.2s ease,
    transform 0.1s ease;
}

.logout-btn:hover {
  background-color: #475569;
}

.logout-btn:active {
  transform: scale(0.96);
}

/* ==========================================================================
   ✨ PREMIUM FINISHING TOUCHES ✨
   ========================================================================== */

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background-color: rgba(148, 163, 184, 0.3);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background-color: rgba(148, 163, 184, 0.6);
}
.subcategory-pills::-webkit-scrollbar,
.item-tabs-container::-webkit-scrollbar {
  display: block;
}

.item-tab {
  transition:
    transform 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s,
    border-bottom-color 0.2s;
}
.item-tab:hover {
  transform: translateY(-2px);
  opacity: 1;
}
.exc-card {
  transition:
    transform 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.2s;
}
.exc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border-color: #cbd5e1;
}

.exc-card {
  position: relative;
  z-index: 1;
  overflow: hidden;
  background: #f8fafc;
}
.exc-card::before {
  content: '"';
  position: absolute;
  top: -15px;
  left: 10px;
  font-family: Georgia, serif;
  font-size: 6rem;
  color: rgba(148, 163, 184, 0.12);
  z-index: -1;
  line-height: 1;
}
.exc-text {
  font-style: italic;
  color: #475569;
  line-height: 1.6;
  font-size: 0.95rem;
}

.card-image-header-v2::before {
  background: linear-gradient(
    to top,
    rgba(15, 23, 42, 0.9) 0%,
    rgba(15, 23, 42, 0.4) 60%,
    transparent 100%
  );
  backdrop-filter: blur(2px);
}
.text-col-image::before {
  background: linear-gradient(
    to top,
    rgba(15, 23, 42, 0.95) 0%,
    rgba(15, 23, 42, 0.5) 50%,
    transparent 100%
  );
  backdrop-filter: blur(2px);
}

.state-message.empty {
  background: white;
  border: 2px dashed #cbd5e1;
  border-radius: 24px;
  max-width: 600px;
  margin: 40px auto;
  padding: 60px 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}
.state-message.empty h2 {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  color: #0f172a;
  font-size: 1.6rem;
}
.state-message.empty h2::before {
  content: '📭';
  font-size: 3.5rem;
  background: #f8fafc;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-bottom: 5px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.02);
}

/* NOTE: Keyword-filter interaction styles were removed with the word-cloud feature. */

/* ==========================================
   ⚙️ ADMIN TABS NAVIGATION STYLES
   ========================================== */
.admin-tabs-nav-bar {
  display: flex;
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  margin-bottom: 28px;
  padding: 2px 2px 6px 2px;
  scrollbar-width: none;
}
.admin-tabs-nav-bar::-webkit-scrollbar {
  display: none;
}
.admin-tabs-container {
  position: relative;
  display: inline-grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  background: #f8fafc;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}
.tab-btn {
  position: relative;
  z-index: 2;
  background: transparent !important;
  border: none;
  padding: 11px 18px;
  font-weight: 600;
  font-size: 0.92rem;
  color: #64748b;
  cursor: pointer;
  transition: color 0.25s ease;
  text-align: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
  user-select: none;
  border-radius: 8px;
}
.tab-btn:hover {
  color: #0f172a;
}
.tab-btn.active {
  color: #ffffff !important;
  font-weight: 700;
}
.tab-btn-icon {
  font-size: 1rem;
  line-height: 1;
  display: inline-flex;
  align-items: center;
}
.tab-btn-text {
  font-size: 0.92rem;
  line-height: 1.2;
  letter-spacing: -0.01em;
}
.sliding-highlight {
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 6px;
  width: calc((100% - 12px) / 5);
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(234, 88, 12, 0.35);
  transition: transform 0.35s cubic-bezier(0.25, 1, 0.5, 1);
  z-index: 1;
  pointer-events: none;
}
.sliding-highlight.analytics {
  transform: translateX(0%);
}
.sliding-highlight.manager {
  transform: translateX(100%);
}
.sliding-highlight.descriptions {
  transform: translateX(200%);
}
.sliding-highlight.questions {
  transform: translateX(300%);
}
.sliding-highlight.statistics {
  transform: translateX(400%);
}

/* ======================================================== */
/* TAB 5: STATISTICAL TESTING & INFERENCE STYLES            */
/* ======================================================== */
.stats-page-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.stats-badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(13, 148, 136, 0.1);
  color: #0d9488;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 4px 10px;
  border-radius: 9999px;
  margin-bottom: 8px;
}

.live-dot-teal {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0d9488;
  box-shadow: 0 0 6px rgba(13, 148, 136, 0.6);
}

.stats-header-kpis {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-mini-pill {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.stat-mini-pill .pill-number {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.stat-mini-pill .pill-label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

.stats-method-toggle-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.method-btn-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.method-toggle-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border: 2px solid transparent;
  background: #f8fafc;
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.method-toggle-btn:hover {
  background: #f1f5f9;
}

.method-toggle-btn.active {
  background: #eff6ff;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.12);
}

.method-toggle-btn .toggle-icon {
  font-size: 2rem;
}

.toggle-text-block strong {
  display: block;
  font-size: 1.05rem;
  color: #0f172a;
  margin-bottom: 2px;
}

.toggle-text-block small {
  display: block;
  font-size: 0.8rem;
  color: #64748b;
}

.stats-config-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  margin-bottom: 24px;
}

.config-header {
  margin-bottom: 20px;
}

.config-header h3 {
  font-size: 1.2rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.config-sub {
  font-size: 0.85rem;
  color: #64748b;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  align-items: flex-end;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-field label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.form-select {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: white;
  color: #0f172a;
  font-size: 0.95rem;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.form-select:focus {
  border-color: #f97316;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.15);
}

.run-test-btn {
  padding: 11px 24px !important;
  font-weight: 700 !important;
  font-size: 0.95rem !important;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.stats-result-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hypothesis-summary-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-left: 6px solid #94a3b8;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.03);
}

.hypothesis-summary-card.is-sig {
  border-left-color: #0d9488;
}

.hypo-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.badge-tag {
  display: inline-block;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 6px;
  margin-bottom: 6px;
}

.hypo-formula {
  font-size: 1rem;
  color: #334155;
  margin: 0;
}

.sig-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 9999px;
  font-size: 0.85rem;
  font-weight: 700;
}

.badge-sig {
  background: #ccfbf1;
  color: #0f766e;
}

.badge-sig .dot-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0d9488;
}

.badge-nonsig {
  background: #f1f5f9;
  color: #64748b;
}

.badge-nonsig .dot-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}

.takeaway-box {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 12px;
  padding: 16px;
}

.takeaway-icon {
  font-size: 1.5rem;
  line-height: 1;
}

.takeaway-text h4 {
  font-size: 0.9rem;
  font-weight: 700;
  color: #92400e;
  text-transform: uppercase;
  margin: 0 0 4px 0;
}

.takeaway-text p {
  font-size: 0.95rem;
  color: #78350f;
  margin: 0;
  line-height: 1.5;
}

.stats-comparison-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 16px;
  align-items: center;
}

.group-stat-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  text-align: center;
}

.card-g-tag {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 4px;
}

.group-stat-card h3 {
  font-size: 1.3rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 12px 0;
}

.g-score-hero {
  display: flex;
  justify-content: center;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 16px;
}

.hero-score {
  font-size: 2.8rem;
  font-weight: 900;
  color: #0f172a;
}

.hero-scale {
  font-size: 1.1rem;
  font-weight: 600;
  color: #94a3b8;
}

.g-metrics-grid {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 0.9rem;
  color: #475569;
}

.vs-divider-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
}

.diff-badge {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 0.95rem;
  color: #334155;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.ci-text {
  font-size: 0.75rem;
  font-family: monospace;
  color: #64748b;
}

.stats-table-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.table-card-header {
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.table-card-header h4 {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.sample-size-explainer {
  margin: 4px 0 0 0;
  font-size: 0.78rem;
  color: #64748b;
  font-weight: 500;
  line-height: 1.4;
  max-width: 650px;
}

.grand-mean-tag {
  font-size: 0.85rem;
  color: #475569;
}

.effect-header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #eff6ff;
  color: #1e40af;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 0.85rem;
}

.effect-sub-badge {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: #3b82f6;
}

.table-card-body {
  overflow-x: auto;
}

.stats-report-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.stats-report-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.stats-report-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9rem;
  color: #1e293b;
}

.stats-report-table tr:last-child td {
  border-bottom: none;
}

.total-row td {
  background: #f8fafc;
  font-weight: 700;
  border-top: 2px solid #e2e8f0;
}

.mini-score-bar-track {
  width: 100px;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.mini-score-fill {
  height: 100%;
  background: #f97316;
  border-radius: 4px;
}

.effect-badge {
  display: inline-block;
  background: #f1f5f9;
  color: #334155;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.font-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.text-teal {
  color: #0d9488 !important;
}

@media (max-width: 840px) {
  .method-btn-group {
    grid-template-columns: 1fr;
  }
  .stats-comparison-row {
    grid-template-columns: 1fr;
  }
}

.description-editor-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.78fr) minmax(0, 1.7fr);
  min-height: 520px;
  border-top: 1px solid #e2e8f0;
}
.description-item-list {
  max-height: 680px;
  overflow-y: auto;
  padding: 16px;
  border-right: 1px solid #e2e8f0;
  background: #fbfcfe;
}
.description-search {
  margin-bottom: 12px;
}
.description-item-option {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: 12px;
  text-align: left;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: #334155;
  cursor: pointer;
}
.description-item-option:hover {
  background: #f1f5f9;
}
.description-item-option.active {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}
.description-option-name {
  font-weight: 700;
  font-size: 0.92rem;
}
.description-option-meta,
.description-status {
  font-size: 0.76rem;
  color: #64748b;
}
.description-status.written {
  color: #15803d;
}
.description-list-empty {
  padding: 18px 8px;
  color: #64748b;
  font-size: 0.88rem;
}
.description-editor {
  padding: 28px 32px;
  min-width: 0;
}
.description-editor-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}
.description-editor-heading h3 {
  margin: 12px 0 5px;
  color: #0f172a;
  font-size: 1.45rem;
}
.description-editor-heading p {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}
.description-updated {
  color: #64748b;
  font-size: 0.78rem;
  text-align: right;
  white-space: nowrap;
}
.description-form label {
  display: block;
  margin-bottom: 8px;
  color: #334155;
  font-size: 0.88rem;
  font-weight: 700;
}
.description-form textarea {
  display: block;
  width: 100%;
  min-height: 170px;
  resize: vertical;
  padding: 13px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  color: #1e293b;
  font: inherit;
  line-height: 1.55;
  box-sizing: border-box;
}
.description-form textarea:focus {
  outline: 3px solid rgba(249, 115, 22, 0.18);
  border-color: #f97316;
}
.description-form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
}
.description-form-footer > span {
  color: #64748b;
  font-size: 0.8rem;
}
.description-preview {
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}
.description-preview-label {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.description-preview p {
  max-width: 70ch;
  margin: 8px 0 0;
  color: #334155;
  line-height: 1.6;
}
.description-fallback-note {
  display: inline-block;
  margin-top: 8px;
  color: #64748b;
  font-size: 0.76rem;
}
.description-editor-empty {
  grid-column: 1 / -1;
  display: grid;
  place-items: center;
  min-height: 300px;
  color: #64748b;
}

@media (max-width: 768px) {
  .admin-tabs-nav-bar {
    margin-bottom: 20px;
  }
  .admin-tabs-container {
    grid-template-columns: repeat(5, minmax(125px, 1fr));
  }
  .tab-btn {
    padding: 10px 10px;
    font-size: 0.82rem;
    gap: 6px;
  }
  .description-editor-layout {
    grid-template-columns: 1fr;
  }
  .description-item-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 4px;
    max-height: 300px;
    border-right: 0;
    border-bottom: 1px solid #e2e8f0;
  }
  .description-search {
    grid-column: 1 / -1;
  }
  .description-editor {
    padding: 22px 18px;
  }
  .description-editor-heading {
    flex-direction: column;
    gap: 10px;
  }
  .description-updated {
    text-align: left;
  }
}

.manager-layout {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}
.manager-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}
.manager-header-row h2 {
  margin: 0;
  font-size: 1.4rem;
  color: #0f172a;
}
.item-manager-header {
  padding: 28px 30px;
}
.manager-description {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 0.9rem;
}
.add-item-btn {
  min-height: 44px;
  white-space: nowrap;
}
.add-item-btn span {
  font-size: 1.25rem;
  line-height: 0;
  vertical-align: -1px;
  margin-right: 5px;
}
.manager-header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.danger-outline {
  background: #fff;
  color: #dc2626;
  border: 1px solid #fecaca;
}
.danger-outline:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #b91c1c;
}
.danger-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.danger-solid {
  background: #dc2626;
  color: #fff;
  border: 1px solid #dc2626;
}
.danger-solid:hover:not(:disabled) {
  background: #b91c1c;
  border-color: #b91c1c;
}
.danger-solid:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.manager-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 30px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}
.search-field {
  display: flex;
  align-items: center;
  gap: 9px;
  flex: 1;
  max-width: 520px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  color: #94a3b8;
  background: #f8fafc;
}
.search-field:focus-within {
  border-color: #f97316;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}
.search-field input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #0f172a;
  font: inherit;
}
.clear-search,
.modal-close {
  border: 0;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}
.clear-search {
  font-size: 1.35rem;
  line-height: 1;
}
.clear-search:hover,
.modal-close:hover {
  color: #0f172a;
}
/* Custom Category Dropdown in Menu Manager Toolbar */
.custom-category-dropdown {
  position: relative;
  display: inline-block;
}

.cat-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 12px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  color: #334155;
  font-family: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.cat-dropdown-trigger:hover {
  border-color: #94a3b8;
  background: #f8fafc;
}

.cat-dropdown-trigger.open,
.cat-dropdown-trigger:focus-visible {
  outline: none;
  border-color: #f97316;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.12);
}

.cat-dropdown-trigger.active {
  border-color: #fdba74;
  background: #fffaf5;
}

.trigger-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger-label {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.trigger-badge.all-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  font-weight: 600;
}

.filter-tag-icon {
  font-size: 0.9rem;
}

.selected-cat-pill {
  font-size: 0.78rem !important;
  padding: 3px 10px !important;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 0.72rem;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 999px;
  line-height: 1;
}

.quick-clear-cat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 50%;
  cursor: pointer;
  font-size: 0.7rem;
  line-height: 1;
  transition: all 0.15s ease;
  margin-left: -2px;
}

.quick-clear-cat:hover {
  background: #ef4444;
  color: #ffffff;
}

.chevron-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  margin-left: 2px;
}

.chevron-icon.rotated {
  transform: rotate(180deg);
  color: #f97316;
}

/* Dropdown Menu Panel */
.cat-dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 120;
  min-width: 290px;
  max-height: 380px;
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 16px 36px -6px rgba(15, 23, 42, 0.14), 0 4px 12px -2px rgba(15, 23, 42, 0.06);
  padding: 8px;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.cat-dropdown-menu::-webkit-scrollbar {
  width: 6px;
}

.cat-dropdown-menu::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.cat-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}

.cat-menu-item:hover {
  background: #f8fafc;
}

.cat-menu-item.selected {
  background: #fff7ed;
}

.cat-menu-item.all-option {
  padding: 9px 10px;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.all-icon {
  font-size: 1rem;
}

.item-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: #1e293b;
}

.cat-preview-pill {
  font-size: 0.78rem !important;
  padding: 3px 10px !important;
  pointer-events: none;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-count-badge {
  font-size: 0.75rem;
  font-weight: 600;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 6px;
}

.cat-menu-item.selected .item-count-badge {
  background: #ffedd5;
  color: #c2410c;
}

.check-mark {
  color: #f97316;
  font-weight: 800;
  font-size: 0.85rem;
}

.menu-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 6px 4px;
}

.menu-section {
  padding: 4px 0;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 4px 10px;
}

.section-badge-icon {
  font-size: 0.85rem;
}

.section-heading {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #64748b;
  text-transform: uppercase;
}

.section-count {
  font-size: 0.7rem;
  color: #94a3b8;
  font-weight: 600;
}

.section-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* Dropdown pop animation */
.dropdown-pop-enter-active,
.dropdown-pop-leave-active {
  transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-pop-enter-from,
.dropdown-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}

/* Modal Form Custom Select & Pill Preview */
.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.form-pill-preview {
  font-size: 0.75rem !important;
  padding: 2px 10px !important;
  pointer-events: none;
}

.custom-select-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.custom-styled-select {
  width: 100%;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 38px !important;
  cursor: pointer;
}

.custom-select-arrow {
  position: absolute;
  right: 14px;
  pointer-events: none;
  color: #94a3b8;
  display: flex;
  align-items: center;
}
.result-count {
  margin-left: auto;
  color: #94a3b8;
  font-size: 0.8rem;
  white-space: nowrap;
}
.manager-empty {
  padding: 70px 20px;
  text-align: center;
  color: #64748b;
}
.empty-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: #fff7ed;
  color: #f97316;
  font-size: 1.7rem;
}
.manager-empty h3 {
  margin: 0 0 5px;
  color: #0f172a;
  font-size: 1.1rem;
}
.manager-empty p {
  margin: 0 auto 18px;
  max-width: 44ch;
  font-size: 0.9rem;
}
.orange-solid {
  background: #f97316;
  color: white;
  border: none;
}
.orange-solid:hover {
  background: #ea580c;
}

.table-container {
  overflow-x: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.data-table th {
  background: #f1f5f9;
  color: #475569;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 15px 25px;
  font-weight: 700;
  border-bottom: 2px solid #e2e8f0;
}
.data-table td {
  padding: 15px 25px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  color: #334155;
}
.data-table tr:hover td {
  background: #f8fafc;
}

.table-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  border: 1px solid #e2e8f0;
}
.fw-bold {
  font-weight: 600;
  color: #0f172a !important;
}
.code-font {
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  color: #64748b;
}
.actions-col {
  text-align: right;
  width: 180px;
}
.action-btn {
  background: white;
  border: 1px solid #cbd5e1;
  min-height: 38px;
  padding: 7px 13px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 8px;
}
.edit-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}
.del-btn {
  color: #ef4444;
  border-color: #fca5a5;
  background: #fef2f2;
}
.del-btn:hover {
  background: #ef4444;
  color: white;
  border-color: #ef4444;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Form Modal Styles */
.form-card {
  max-width: 560px;
  text-align: left;
}
.modal-heading-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.modal-kicker {
  display: block;
  margin-bottom: 6px;
  color: #f97316;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.modal-close {
  padding: 0 2px;
  font-size: 1.7rem;
  line-height: 1;
}
.form-intro {
  margin: -5px 0 24px !important;
  font-size: 0.9rem !important;
}
.required-mark,
.optional-mark {
  margin-left: 5px;
  font-size: 0.7rem;
  font-weight: 500;
}
.required-mark {
  color: #f97316;
}
.optional-mark {
  color: #94a3b8;
}
.upload-btn {
  min-height: 40px;
}
.admin-toast {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 10000;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: min(380px, calc(100vw - 40px));
  padding: 13px 17px;
  border-radius: 10px;
  color: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
  animation: toastIn 0.2s ease-out;
  font-size: 0.9rem;
  font-weight: 600;
}
.toast-success {
  background: #15803d;
}
.toast-error {
  background: #b91c1c;
}
.admin-toast > span {
  display: grid;
  place-items: center;
  width: 21px;
  height: 21px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
}
@keyframes toastIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  .item-manager-header,
  .manager-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .item-manager-header {
    gap: 18px;
  }
  .manager-header-actions {
    flex-direction: column;
  }
  .manager-header-actions .nav-btn {
    width: 100%;
  }
  .add-item-btn {
    width: 100%;
  }
  .manager-toolbar {
    padding: 16px 20px;
  }
  .search-field {
    max-width: none;
  }
  .custom-category-dropdown {
    width: 100%;
  }
  .cat-dropdown-trigger {
    width: 100%;
    justify-content: space-between;
  }
  .cat-dropdown-menu {
    width: 100%;
    min-width: unset;
  }
  .result-count {
    margin-left: 0;
  }
  .data-table thead {
    display: none;
  }
  .data-table,
  .data-table tbody,
  .data-table tr,
  .data-table td {
    display: block;
    width: 100%;
  }
  .data-table tr {
    position: relative;
    padding: 18px 20px 18px 86px;
    min-height: 94px;
    border-bottom: 1px solid #e2e8f0;
  }
  .data-table td {
    padding: 2px 0;
    border: 0;
  }
  .data-table td:first-child {
    position: absolute;
    left: 20px;
    top: 20px;
    width: auto;
  }
  .data-table td:nth-child(3) {
    margin-top: 5px;
  }
  .data-table td:nth-child(4) {
    display: none;
  }
  .data-table .actions-col {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    text-align: left;
  }
  .data-table .action-btn {
    margin-left: 0;
    flex: 0 0 auto;
  }
  .admin-toast {
    right: 20px;
    bottom: 20px;
  }
}
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}
.form-input {
  padding: 12px 15px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 1rem;
  color: #0f172a;
  background: #f8fafc;
  transition: all 0.2s;
}
.form-input:focus {
  outline: none;
  border-color: #f97316;
  background: white;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}
.image-upload-box {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 15px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}
.preview-thumb {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
  border: 1px solid #e2e8f0;
}
.preview-empty {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  border: 1px dashed #94a3b8;
}
.mt-4 {
  margin-top: 1.5rem;
}
.mb-4 {
  margin-bottom: 1.5rem;
}
.mb-2 {
  margin-bottom: 0.5rem;
}
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
/* Question Manager 2-Section Styles */
.qm-section-group {
  margin-bottom: 2.5rem;
}
.qm-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 18px;
  border: 1px solid #e2e8f0;
}
.qm-section-header.demo-header {
  background: linear-gradient(to right, #eff6ff, #f8fafc);
  border-color: #bfdbfe;
}
.qm-section-header.menu-header {
  background: linear-gradient(to right, #fff7ed, #f8fafc);
  border-color: #fed7aa;
}
.qm-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.qm-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}
.qm-icon-box.blue-box {
  background: #dbeafe;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.qm-icon-box.orange-box {
  background: #ffedd5;
  color: #ea580c;
  border: 1px solid #fed7aa;
}
.qm-section-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
}
.qm-section-desc {
  margin: 2px 0 0;
  font-size: 0.85rem;
  color: #64748b;
}
.qm-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.qm-badge {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 20px;
  white-space: nowrap;
}
.qm-badge.blue-pill {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.qm-badge.orange-pill {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}
.add-q-sub-btn {
  padding: 8px 16px !important;
  font-size: 0.85rem !important;
  border-radius: 8px;
}
.demo-q-card {
  border-top: 4px solid #3b82f6 !important;
}
.menu-q-card {
  border-top: 4px solid #f97316 !important;
}
.q-card-top-row {
  background: white !important;
  border-bottom: none !important;
  padding: 16px 20px !important;
}
.q-card-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}
.demo-q-num {
  position: static !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  background: #eff6ff !important;
  color: #1d4ed8 !important;
  border: 1px solid #bfdbfe !important;
}
.q-title-text {
  font-size: 1.1rem;
  color: #0f172a;
  display: block;
}
.q-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.demo-scope-badge {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.fixed-indicator-badge {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 6px;
}
.q-card-options-row {
  padding: 0 20px 20px 64px;
}
.options-pills-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.pill-demo {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1e40af;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  pointer-events: none;
}
.opt-num {
  font-weight: 800;
  color: #3b82f6;
  font-size: 0.8rem;
}
.del-opt-btn {
  background: transparent;
  border: none;
  color: #ef4444;
  cursor: pointer;
  font-weight: bold;
  font-size: 0.85rem;
  line-height: 1;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 2px;
  padding: 0;
  transition: all 0.15s ease;
  pointer-events: auto;
  position: relative;
  z-index: 2;
}
.del-opt-btn:hover {
  background: #fee2e2;
  color: #dc2626;
  transform: scale(1.15);
}
.edit-dim-btn {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 0.8rem;
  line-height: 1;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  padding: 0;
  transition: all 0.15s ease;
  pointer-events: auto;
  position: relative;
  z-index: 2;
}
.edit-dim-btn:hover {
  background: #ffedd5;
  color: #ea580c;
  transform: scale(1.15);
}
.menu-q-num {
  position: static !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  background: #fff7ed !important;
  color: #c2410c !important;
  border: 1px solid #fed7aa !important;
}
.prompt-text {
  font-style: italic;
  color: #475569;
  font-size: 0.9rem;
}
.q-card-scale-row {
  padding: 0 20px 14px 64px;
}
.scale-pills-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.scale-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.8rem;
}
.scale-val {
  color: #ea580c;
  font-weight: 800;
}
.scale-lbl {
  color: #334155;
}
.pill-matrix-dim {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  pointer-events: auto;
}
.opt-num-orange {
  font-weight: 800;
  color: #ea580c;
  font-size: 0.8rem;
}
.grid-req-note {
  font-size: 0.8rem;
  color: #64748b;
}
.grid-req-note .req-asterisk {
  color: #ef4444;
  font-weight: bold;
}
.custom-questions-divider {
  border-top: 1px dashed #cbd5e1;
  padding-top: 16px;
  margin-top: 24px;
  margin-bottom: 16px;
}

/* ==========================================================================
   SECTION 1: DEMOGRAPHIC OVERVIEW PANEL
   ========================================================================== */
.demographics-overview-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  margin-bottom: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.demo-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.demo-panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.demo-panel-title-wrap h3 {
  margin: 0 0 2px 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #0f172a;
}

.demo-panel-sub {
  margin: 0;
  font-size: 0.82rem;
  color: #64748b;
}

.demo-panel-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 22px 24px;
}

@media (max-width: 900px) {
  .demo-panel-content {
    grid-template-columns: 1fr;
  }
}

.demo-stat-card {
  background: #fdfdfd;
  border: 1px solid #eef2f6;
  border-radius: 12px;
  padding: 16px 18px;
}

.demo-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.demo-icon {
  font-size: 1.3rem;
}

.demo-card-top h4 {
  margin: 0 0 2px 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
}

.demo-hint {
  margin: 0;
  font-size: 0.76rem;
  color: #94a3b8;
}

.demo-bars-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.demo-bar-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.demo-bar-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.82rem;
  color: #334155;
}

.demo-opt-label {
  font-weight: 500;
}

.demo-opt-val {
  color: #64748b;
  font-size: 0.8rem;
}

.demo-opt-val strong {
  color: #0f172a;
}

.demo-track {
  width: 100%;
  height: 8px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.demo-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease-out;
}

.demo-fill.blue-theme {
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
}

.demo-fill.orange-theme {
  background: linear-gradient(90deg, #fb923c, #f97316);
}

/* ==========================================================================
   ITEM SUMMARY HEADER CARD
   ========================================================================== */
.item-analytics-view {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.item-summary-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  gap: 20px;
  flex-wrap: wrap;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 18px;
  flex: 1;
  min-width: 280px;
}

.summary-thumb {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  background-color: #f1f5f9;
  background-size: cover;
  background-position: center;
  border: 2px solid #e2e8f0;
  flex-shrink: 0;
}

.summary-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-badges {
  display: flex;
  gap: 8px;
  align-items: center;
}

.eval-badge {
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.78rem;
}

.summary-title {
  margin: 2px 0 0 0;
  font-size: 1.45rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.summary-desc {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
  max-width: 540px;
  line-height: 1.4;
}

.summary-metrics {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric-pill-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 90px;
}

.metric-num {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.metric-lbl {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  font-weight: 600;
  margin-top: 2px;
}

/* ==========================================================================
   SECTION 2: GRID EVALUATION CARDS (QUESTION 1 & 2)
   ========================================================================== */
.section2-grid-columns {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.grid-card-container {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.grid-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #fafafa;
  border-bottom: 1px solid #eef2f6;
  gap: 14px;
  flex-wrap: wrap;
}

.head-title-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
}

.q-badge {
  background: #f97316;
  color: white;
  font-size: 0.85rem;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 8px;
  letter-spacing: 0.03em;
}

.head-title-wrap h3 {
  margin: 0 0 2px 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
}

.grid-card-subtitle {
  margin: 0;
  font-size: 0.82rem;
  color: #64748b;
}

.evaluator-pill {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #334155;
}

.grid-card-content {
  padding: 10px 24px 20px 24px;
}

.grid-col-headers {
  display: grid;
  grid-template-columns: 2.2fr 1fr 2.5fr 1fr 0.8fr;
  gap: 14px;
  padding: 12px 14px;
  font-size: 0.74rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 700;
  color: #94a3b8;
  border-bottom: 1px solid #f1f5f9;
}

.grid-rows-list {
  display: flex;
  flex-direction: column;
}

.grid-row-item {
  display: grid;
  grid-template-columns: 2.2fr 1fr 2.5fr 1fr 0.8fr;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border-bottom: 1px solid #f8fafc;
  transition: background-color 0.15s;
  border-radius: 8px;
}

.grid-row-item:hover {
  background-color: #f8fafc;
}

.grid-row-item.leader-row {
  background-color: #fffaf0;
  border-left: 3px solid #f97316;
}

.row-info-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.row-mood-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.92rem;
  color: #0f172a;
}

.top-tag {
  background: #fef3c7;
  color: #b45309;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}

.row-mood-desc {
  font-size: 0.76rem;
  color: #64748b;
  line-height: 1.3;
}

.row-score-col {
  display: flex;
  align-items: center;
}

.score-chip {
  font-size: 0.88rem;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.score-chip.score-high {
  background: #ecfdf5;
  color: #047857;
}

.score-chip.score-med {
  background: #fefce8;
  color: #854d0e;
}

.score-chip.score-low {
  background: #fef2f2;
  color: #b91c1c;
}

.row-bar-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rating-track-bar {
  width: 100%;
  height: 10px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.rating-track-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #fb923c, #ea580c);
  transition: width 0.4s ease-out;
}


.mini-scale-ticks {
  display: flex;
  justify-content: space-between;
  font-size: 0.65rem;
  color: #cbd5e1;
  font-weight: 600;
  padding: 0 2px;
}

/* Per-rating vote spread under each score track (e.g. "4:7" = seven 4★ votes).
   Hovering shows the full 1-5 breakdown. */
.dist-ticks {
  color: #64748b;
  font-variant-numeric: tabular-nums;
}
.dist-ticks span {
  white-space: nowrap;
}
.dist-ticks .dist-zero {
  color: #cbd5e1;
  font-weight: 500;
}

.row-suit-col {
  display: flex;
  align-items: center;
}

.suit-badge {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
}

.suit-badge.suit-high {
  background: #dcfce7;
  color: #15803d;
}

.suit-badge.suit-med {
  background: #fef9c3;
  color: #a16207;
}

.suit-badge.suit-low {
  background: #fee2e2;
  color: #991b1b;
}

.row-votes-col {
  display: flex;
  align-items: center;
}

.votes-badge {
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
}

.grid-insight-footer {
  margin-top: 14px;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.85rem;
  color: #334155;
}

.insight-lamp {
  font-size: 1.2rem;
  flex-shrink: 0;
}

/* ==========================================================================
   INDIVIDUAL SURVEY TAKER SUBMISSIONS LOG
   ========================================================================== */
.responses-log-section {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.log-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.log-title-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.log-emoji {
  font-size: 1.5rem;
}

.log-title-area h3 {
  margin: 0 0 2px 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
}

.log-desc {
  margin: 0;
  font-size: 0.82rem;
  color: #64748b;
}

.log-badge {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 20px;
}

.log-empty {
  text-align: center;
  padding: 30px;
  color: #94a3b8;
  font-style: italic;
  font-size: 0.9rem;
}

.log-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.submission-card {
  background: #fdfdfd;
  border: 1px solid #eef2f6;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.15s, box-shadow 0.15s;
}

.submission-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.sub-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.respondent-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  font-size: 1.15rem;
}

.respondent-title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: #0f172a;
}

.respondent-sub {
  font-size: 0.72rem;
  color: #94a3b8;
  font-family: monospace;
}

.sub-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sub-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.sub-group-title {
  margin: 0;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
  color: #64748b;
}

.rating-pills-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.eval-pill {
  font-size: 0.75rem;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.eval-pill.pill-pos {
  background: #f0fdf4;
  color: #166534;
  border-color: #bbf7d0;
}

.eval-pill.pill-neu {
  background: #fefce8;
  color: #854d0e;
  border-color: #fef08a;
}

.eval-pill.pill-neg {
  background: #fef2f2;
  color: #991b1b;
  border-color: #fecaca;
}

.eval-pill.weather {
  background: #f0f9ff;
  border-color: #bae6fd;
  color: #0369a1;
}

.text-group {
  background: #f8fafc;
  padding: 8px 10px;
  border-radius: 6px;
  border-left: 2px solid #3b82f6;
}

.user-text-quote {
  margin: 2px 0 0 0;
  font-size: 0.8rem;
  color: #334155;
  font-style: italic;
  line-height: 1.35;
}

/* ==========================================================================
   📱 COMPREHENSIVE MOBILE RESPONSIVENESS FOR ADMIN DASHBOARD
   ========================================================================== */

/* RETURN & VIEW SURVEY BUTTONS */
.login-return-btn {
  display: inline-block;
  margin-top: 12px;
  color: #64748b;
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 600;
  transition: color 0.2s;
}
.login-return-btn:hover {
  color: #ea580c;
}

.view-survey-btn {
  background: white;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 10px 16px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.view-survey-btn:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
  color: #0f172a;
}

@media (max-width: 992px) {
  .dashboard-layout {
    padding: 24px 20px;
  }
  .cards-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-layout {
    padding: 16px 12px;
  }

  /* Login screen */
  .login-card {
    padding: 30px 22px;
    border-radius: 16px;
    width: 92%;
  }
  .login-card h2 {
    font-size: 1.5rem;
  }

  /* Top Header */
  .top-header {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
    margin-bottom: 20px;
  }
  .header-left {
    gap: 12px;
  }
  .logo-icon {
    width: 38px;
    height: 38px;
    font-size: 1.25rem;
  }
  .top-header h1 {
    font-size: 1.25rem;
    line-height: 1.25;
  }
  .subtitle {
    font-size: 0.8rem;
  }
  .header-right {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
  }
  .header-right button,
  .header-right .view-survey-btn {
    flex: 1 1 calc(50% - 6px);
    justify-content: center;
    min-height: 40px;
    padding: 8px 10px;
    font-size: 0.82rem;
    white-space: nowrap;
  }
  .live-badge {
    flex: 1 1 100%;
    justify-content: center;
    padding: 6px 12px;
    font-size: 0.8rem;
  }

  /* Admin Tabs */
  .admin-tabs-container {
    width: max-content;
    margin-bottom: 20px;
  }
  .tab-btn {
    padding: 10px 8px;
    font-size: 0.8rem;
    gap: 6px;
  }

  /* KPI Grid */
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .global-card {
    grid-column: span 2;
  }
  .new-kpi-card {
    padding: 14px 14px;
    border-radius: 12px;
  }
  .kpi-val {
    font-size: 30px;
  }
  .kpi-name {
    font-size: 10px;
  }
  .kpi-desc {
    font-size: 11px;
  }

  /* Category & Item Filters */
  .navigation-panel {
    margin-bottom: 20px;
    border-radius: 14px;
  }
  .filters-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 12px 14px;
  }
  .category-toggle {
    width: 100%;
    box-sizing: border-box;
  }
  .category-toggle button {
    padding: 8px 12px;
    font-size: 0.88rem;
  }
  .divider {
    display: none;
  }
  .subcategory-pills {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 6px;
    -webkit-overflow-scrolling: touch;
  }
  .item-tabs-container {
    padding: 10px 12px;
    -webkit-overflow-scrolling: touch;
  }
  .item-tab {
    min-width: 80px;
    padding: 6px;
  }
  .tab-thumb {
    width: 38px;
    height: 38px;
  }
  .tab-title {
    font-size: 0.76rem;
    max-width: 80px;
  }

  /* Item Summary Card */
  .item-summary-card {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
    gap: 14px;
    border-radius: 14px;
  }
  .summary-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    min-width: unset;
  }
  .summary-thumb {
    width: 60px;
    height: 60px;
    border-radius: 12px;
  }
  .summary-title {
    font-size: 1.2rem;
  }
  .summary-metrics {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .metric-pill-box {
    min-width: unset;
    padding: 8px 4px;
    border-radius: 10px;
  }
  .metric-num {
    font-size: 0.98rem;
  }
  .metric-lbl {
    font-size: 0.65rem;
  }

  /* Section 2 Grid Evaluation Table (Scrollable on phones) */
  .grid-card-container {
    border-radius: 14px;
  }
  .grid-card-head {
    padding: 14px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .head-title-wrap {
    gap: 10px;
  }
  .head-title-wrap h3 {
    font-size: 1.05rem;
  }
  .evaluator-pill {
    align-self: flex-start;
  }
  .grid-card-content {
    padding: 8px 12px 14px 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .grid-col-headers,
  .grid-row-item {
    min-width: 560px;
  }

  /* Responses Log */
  .responses-log-section {
    padding: 16px 14px;
    border-radius: 14px;
  }
  .log-section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .log-cards-grid {
    grid-template-columns: 1fr;
  }

  /* Question Manager Mobile */
  .qm-section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 14px;
  }
  .qm-header-right {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }
  .q-card-top-row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    padding: 14px !important;
  }
  .q-card-options-row,
  .q-card-scale-row {
    padding: 0 14px 14px 14px !important;
  }

  /* Form & Confirmation Modals */
  .modal-card {
    padding: 26px 18px;
    border-radius: 18px;
  }
  .modal-card h2 {
    font-size: 1.4rem;
  }
  .modal-card p {
    font-size: 0.95rem;
  }
  .modal-actions {
    flex-direction: column-reverse;
    gap: 10px;
  }
  .modal-actions button {
    width: 100%;
  }
  .form-card {
    max-width: 95%;
    padding: 22px 16px !important;
  }
}

@media (max-width: 480px) {
  .dashboard-layout {
    padding: 12px 10px;
  }
  .top-header h1 {
    font-size: 1.15rem;
  }
  .tab-btn {
    font-size: 0.78rem;
    padding: 8px 6px;
  }
  .kpi-val {
    font-size: 26px;
  }
  .header-right button,
  .header-right .view-survey-btn {
    flex: 1 1 100%;
  }
}

</style>
