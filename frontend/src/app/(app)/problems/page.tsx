'use client';

import { useQuery } from '@tanstack/react-query';
import { LibraryIcon, SearchIcon } from 'lucide-react';
import Link from 'next/link';
import * as React from 'react';

import { EmptyState } from '@/components/empty-state';
import { ImportProblemDialog } from '@/components/problems/import-dialog';
import { DifficultyBadge } from '@/components/verdict-badge';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

export default function ProblemsPage() {
  const [search, setSearch] = React.useState('');
  const [difficulty, setDifficulty] = React.useState('ALL');
  const [debounced, setDebounced] = React.useState('');

  React.useEffect(() => {
    const timer = setTimeout(() => setDebounced(search), 250);
    return () => clearTimeout(timer);
  }, [search]);

  const problems = useQuery({
    queryKey: queryKeys.problems(debounced, difficulty),
    queryFn: () => api.listProblems({ search: debounced || undefined, difficulty, size: 50 }),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Problems</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            A curated starter library, plus everything you import.
          </p>
        </div>
        <ImportProblemDialog />
      </div>

      <div className="flex flex-wrap gap-2">
        <div className="relative min-w-56 flex-1">
          <SearchIcon className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            className="pl-9"
            placeholder="Search problems…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <Select value={difficulty} onValueChange={setDifficulty}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All difficulties</SelectItem>
            <SelectItem value="EASY">Easy</SelectItem>
            <SelectItem value="MEDIUM">Medium</SelectItem>
            <SelectItem value="HARD">Hard</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {problems.isLoading ? (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-32" />
          ))}
        </div>
      ) : problems.data && problems.data.items.length > 0 ? (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {problems.data.items.map((problem) => (
            <Link key={problem.id} href={`/workspace/${problem.id}`} className="group">
              <Card className="h-full transition-all group-hover:border-primary/40 group-hover:shadow-md">
                <CardContent className="flex h-full flex-col p-4">
                  <div className="flex items-start justify-between gap-2">
                    <h2 className="font-medium tracking-tight text-pretty">{problem.title}</h2>
                    <DifficultyBadge difficulty={problem.difficulty} />
                  </div>
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {problem.topics.slice(0, 3).map((topic) => (
                      <Badge key={topic} variant="muted" className="font-normal">
                        {topic}
                      </Badge>
                    ))}
                    {problem.topics.length > 3 && (
                      <Badge variant="outline" className="font-normal">
                        +{problem.topics.length - 3}
                      </Badge>
                    )}
                  </div>
                  <p className="text-muted-foreground mt-auto pt-3 text-xs">
                    {problem.curated ? 'Curated library' : `Imported from ${problem.source.toLowerCase()}`}
                  </p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={LibraryIcon}
          title="No problems match"
          description="Clear the filters, or import the problem you are working on right now."
          action={<ImportProblemDialog />}
        />
      )}
    </div>
  );
}
